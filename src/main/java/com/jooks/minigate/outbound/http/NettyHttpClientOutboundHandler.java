package com.jooks.minigate.outbound.http;

import com.jooks.minigate.filter.HeaderHttpResponseFilter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

public class NettyHttpClientOutboundHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final ChannelHandlerContext parentCtx;

    public NettyHttpClientOutboundHandler(final ChannelHandlerContext ctx) {
        this.parentCtx = ctx;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("已与 " + ctx.channel().remoteAddress() + " 建立连接");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
            throws Exception {
        FullHttpResponse fullHttpResponse = (FullHttpResponse) msg;
        DefaultFullHttpResponse newResponse = new DefaultFullHttpResponse(fullHttpResponse.protocolVersion(), fullHttpResponse.status(), fullHttpResponse.content().copy());
        newResponse.headers().set(fullHttpResponse.headers());

        new HeaderHttpResponseFilter().filter(fullHttpResponse);

        parentCtx.writeAndFlush(newResponse);
        ctx.close();
    }
}