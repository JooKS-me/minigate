package com.jooks.minigate.outbound.http;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jooks.minigate.filter.LoggingHttpRequestFilter;
import com.jooks.minigate.router.HttpEndpointRouter;
import com.jooks.minigate.router.RandomHttpEndpointRouter;
import com.jooks.minigate.router.RoundRobinHttpEndpointRouter;
import com.jooks.minigate.router.RouterRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MemoryAttribute;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于Netty实现的HTTP请求客户端
 */
@Slf4j
public class NettyHttpClient {

    HttpEndpointRouter router;

    private static final EventLoopGroup clientGroup = new NioEventLoopGroup(14, new ThreadFactoryBuilder().setNameFormat("client work-%d").build());

    public void handle(final FullHttpRequest request, final ChannelHandlerContext ctx, String balance) throws Exception {
        if (router == null) {
            if (balance.contains("robin")) {
                router = new RoundRobinHttpEndpointRouter();
            } else if (balance.contains("random")) {
                router = new RandomHttpEndpointRouter();
            } else {
                log.error("不支持该负载均衡算法");
                ctx.close();
                return;
            }
        }

        List<String> urls = RouterRegistry.getInstance().get(new URI(request.uri()).getPath());
        // 若路由注册中心没有配置url，则不再继续
        if (urls == null) {
            HttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.NOT_FOUND);
            response.headers().set("Connection", "close");
            ctx.writeAndFlush(response);
            ctx.close();
            return;
        }
        // 通过路由得到一个要请求的url
        String url = router.route(urls);
        URI uri = new URI(url);

        Bootstrap b = new Bootstrap();
        b.group(clientGroup);
        b.channel(NioSocketChannel.class);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new HttpClientCodec());
                pipeline.addLast(new HttpContentDecompressor());
                // HttpObjectAggregator是为了避免收到HttpContent
                pipeline.addLast(new HttpObjectAggregator(1024 * 1024));
                pipeline.addLast(new NettyHttpClientOutboundHandler(ctx));
            }
        });

        // 开启客户端
        ChannelFuture f = b.connect(uri.getHost(), uri.getPort()).sync();

        // 解析路径，生成目标uri
        QueryStringEncoder getEncoder = new QueryStringEncoder(url);
        QueryStringDecoder oldDecoder = new QueryStringDecoder(request.uri());
        oldDecoder.parameters().forEach((key, value) -> {
            getEncoder.addParam(key, value.get(0));
        });
        URI targetUri = new URI(getEncoder.toString());

        // 产生一个新的request，用来请求被代理的服务
        HttpRequest newRequest = new DefaultFullHttpRequest(
                request.protocolVersion(), request.method(), targetUri.getRawPath() + "?" + targetUri.getQuery());

        // 添加基本的Header
        newRequest.headers().set(HttpHeaderNames.HOST, uri.getHost());
        newRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        newRequest.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

        // 如果发来的是POST请求，则需要先解析请求体，并加到新的request中
        if (request.method() == HttpMethod.POST) {
            // 给新的request加上请求体
            newRequest = parsePostBody(request, newRequest).finalizeRequest();
        }

        // 打下日志
        new LoggingHttpRequestFilter().filter(newRequest);

        f.channel().writeAndFlush(newRequest);
        f.channel().closeFuture().sync();
    }

    /**
     * 将旧的POST请求中的请求体，迁移到新的POST请求中
     *
     * @param oldRequest 旧请求
     * @param newRequest 新请求
     * @return HttpPostRequestEncoder
     * @throws HttpPostRequestEncoder.ErrorDataEncoderException HttpPostRequestEncoder.ErrorDataEncoderException
     */
    private HttpPostRequestEncoder parsePostBody(final FullHttpRequest oldRequest, HttpRequest newRequest) throws HttpPostRequestEncoder.ErrorDataEncoderException {
        Map<String, String> paramMap = new HashMap<>();
        HttpPostRequestEncoder httpPostRequestEncoder = new HttpPostRequestEncoder(newRequest, false);

        String contentType = oldRequest.headers().get("Content-Type").trim().toLowerCase();

        if (contentType.contains("x-www-form-urlencoded")) {
            // x-www-form-urlencoded 格式的处理
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), oldRequest);
            List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();
            for (InterfaceHttpData parm : parmList) {
                if (parm.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    MemoryAttribute data = (MemoryAttribute) parm;
                    paramMap.put(data.getName(), data.getValue());
                }
            }
        } else if (contentType.contains("json")) {
            // json格式的处理
            Gson gson = new Gson();
            paramMap = gson.fromJson(oldRequest.content().toString(StandardCharsets.UTF_8), new TypeToken<Map<String, String>>() {
            }.getType());
        }

        // 将解析到的参数再次编码
        paramMap.forEach((key, value) -> {
            try {
                httpPostRequestEncoder.addBodyAttribute(key, value);
            } catch (HttpPostRequestEncoder.ErrorDataEncoderException e) {
                System.err.println("参数添加失败");
                e.printStackTrace();
            }
        });

        return httpPostRequestEncoder;
    }
}