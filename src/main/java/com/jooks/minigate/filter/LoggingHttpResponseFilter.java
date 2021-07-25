package com.jooks.minigate.filter;

import io.netty.handler.codec.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingHttpResponseFilter implements HttpResponseFilter {

    @Override
    public void filter(HttpResponse response) {
        log.info("网关收到HTTP响应: status->[" + response.status() + "], header->["
                + response.headers() + "], hashcode->["
                + response.hashCode() + "]");
    }
}
