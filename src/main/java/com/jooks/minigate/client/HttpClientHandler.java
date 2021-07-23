package com.jooks.minigate.client;

import com.jooks.minigate.message.CreatResponse;
import com.jooks.minigate.message.HttpResponseMessage;
import com.jooks.minigate.message.MessageCenter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

public class HttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        MessageCenter.getInstance().getAsyncEventBus().post(new HttpResponseMessage(CreatResponse.createResponse(msg)));
    }
}
