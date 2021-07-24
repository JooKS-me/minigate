package com.jooks.minigate.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeaderHttpRequestFilter implements HttpRequestFilter {

    @Override
    public void filter(HttpRequest httpRequest, ChannelHandlerContext ctx) {
        log.info("请求的filter");
    }
}
