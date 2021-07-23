package com.jooks.minigate.server;

import com.google.common.eventbus.Subscribe;
import com.jooks.minigate.message.CreatResponse;
import com.jooks.minigate.message.HttpResponseMessage;
import com.jooks.minigate.message.MessageCenter;
import com.jooks.minigate.message.MessageRegistry;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<Object> {

    static ChannelHandlerContext channelHandlerContext;

    boolean isKeepAlive;

    public ServerHandler() {
        MessageCenter.getInstance().getAsyncEventBus().register(this);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        channelHandlerContext = ctx;
        HttpRequest request = (HttpRequest) msg;
        this.isKeepAlive = HttpUtil.isKeepAlive(request);
        MessageRegistry.getInstance().put(String.valueOf(ctx.channel().hashCode()), request);
        MessageCenter.getInstance().getAsyncEventBus().post(String.valueOf(ctx.channel().hashCode()));
    }

    @Subscribe
    public void parseResponse(HttpResponseMessage httpResponseMessage) {
        HttpObject msg = httpResponseMessage.getHttpObject();
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;

            System.err.println("STATUS: " + response.status());
            System.err.println("VERSION: " + response.protocolVersion());
            System.err.println();

            if (!response.headers().isEmpty()) {
                for (CharSequence name: response.headers().names()) {
                    for (CharSequence value: response.headers().getAll(name)) {
                        System.err.println("HEADER: " + name + " = " + value);
                    }
                }
                System.err.println();
            }

            if (HttpUtil.isTransferEncodingChunked(response)) {
                System.err.println("CHUNKED CONTENT {");
            } else {
                System.err.println("CONTENT {");
            }

            channelHandlerContext.write(msg);
        }
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;

            System.err.print(content.content().toString(CharsetUtil.UTF_8));
            System.err.flush();

            if (content instanceof LastHttpContent) {
                System.err.println("} END OF CONTENT");
                channelHandlerContext.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.info("ServerHandler发生异常: {}", cause.getMessage());
        ctx.close();
    }
}
