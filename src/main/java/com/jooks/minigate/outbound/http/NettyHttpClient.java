package com.jooks.minigate.outbound.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jooks.minigate.filter.HeaderHttpRequestFilter;
import com.jooks.minigate.filter.HttpRequestFilter;
import com.jooks.minigate.router.HttpEndpointRouter;
import com.jooks.minigate.router.RandomHttpEndpointRouter;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NettyHttpClient {

    private final List<String> proxyServers;

    private final HttpEndpointRouter router = new RandomHttpEndpointRouter();

    public NettyHttpClient(List<String> proxyServers) {
        this.proxyServers = proxyServers;
    }

    public void handle(final FullHttpRequest request, final ChannelHandlerContext ctx, HttpRequestFilter filter) throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        // 经过路由得到一个目标url
        String url = router.route(proxyServers);
        URI uri = new URI(url);
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new HttpClientCodec());
                    ch.pipeline().addLast(new HttpContentDecompressor());
                    // HttpObjectAggregator是为了避免收到HttpContent
                    ch.pipeline().addLast(new HttpObjectAggregator(1024 * 1024));
                    ch.pipeline().addLast(new NettyHttpClientOutboundHandler(ctx));
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

            new HeaderHttpRequestFilter().filter(newRequest, ctx);
            f.channel().writeAndFlush(newRequest);
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 将旧的POST请求中的请求体，迁移到新的POST请求中
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
            String json = oldRequest.content().toString(StandardCharsets.UTF_8);
            paramMap = gson.fromJson(json ,new TypeToken<Map<String,String>>() {}.getType());
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