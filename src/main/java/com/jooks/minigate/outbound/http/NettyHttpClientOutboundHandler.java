package com.jooks.minigate.outbound.http;

import com.jooks.minigate.filter.LoggingHttpResponseFilter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;

public class NettyHttpClientOutboundHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final ChannelHandlerContext parentCtx;

    public NettyHttpClientOutboundHandler(final ChannelHandlerContext ctx) {
        this.parentCtx = ctx;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
            throws Exception {
        FullHttpResponse fullHttpResponse = (FullHttpResponse) msg;
        DefaultFullHttpResponse newResponse = new DefaultFullHttpResponse(fullHttpResponse.protocolVersion(), fullHttpResponse.status(), fullHttpResponse.content().copy());
        newResponse.headers().set(fullHttpResponse.headers());

        // 打下日志
        new LoggingHttpResponseFilter().filter(fullHttpResponse);

        parentCtx.writeAndFlush(newResponse);
        ctx.close();
        parentCtx.close();
    }
}