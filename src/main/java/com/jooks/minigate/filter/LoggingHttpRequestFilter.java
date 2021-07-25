package com.jooks.minigate.filter;

import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingHttpRequestFilter implements HttpRequestFilter {

    @Override
    public void filter(HttpRequest httpRequest) {
        log.info("网关发起HTTP请求: uri->[" + httpRequest.uri() + "], method->["
                + httpRequest.method() + "], header->["
                + httpRequest.headers() + "], hashcode->["
                + httpRequest.hashCode() + "]");
    }
}
