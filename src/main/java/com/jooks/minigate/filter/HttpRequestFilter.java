package com.jooks.minigate.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

public interface HttpRequestFilter {
    
    void filter(HttpRequest httpRequest, ChannelHandlerContext ctx);
    
}
