package com.jooks.minigate.inbound;

import com.jooks.minigate.outbound.http.NettyHttpClient;
import com.jooks.minigate.utils.JwtUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HttpInboundHandler extends ChannelInboundHandlerAdapter {

    private static final ThreadPoolExecutor EXECUTORS = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);

    private final NettyHttpClient handler;

    private String balance;

    private String secret;

    public HttpInboundHandler(String balance, String secret) {
        this.handler = new NettyHttpClient();
        this.balance = balance;
        this.secret = secret;
    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            FullHttpRequest fullRequest = (FullHttpRequest) msg;

            // jwt过滤
            List<String> jwtList = JwtUtils.getInstance().getJwtPath();
            URI uri = new URI(fullRequest.uri());
            if (jwtList.contains(uri.getPath())) {
                String token = fullRequest.headers().get("token");
                if (JwtUtils.getInstance().getClaimByToken(token, secret) == null) {
                    StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append("{\n")
                            .append("  \"code\": 403,\n")
                            .append("    \"message\": \"缺少token\"")
                            .append("}");
                    ByteBuf buf = Unpooled.copiedBuffer(stringBuffer.toString(), StandardCharsets.UTF_8);
                    HttpResponse httpResponse = new DefaultFullHttpResponse(fullRequest.protocolVersion(), HttpResponseStatus.FORBIDDEN, buf);
                    httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                    ctx.writeAndFlush(httpResponse);
                    ctx.close();
                    return;
                }
            }

            fullRequest.content().retain(1);
            // 处理请求
            EXECUTORS.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        handler.handle(fullRequest, ctx, balance);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
