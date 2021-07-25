package com.jooks.minigate.filter;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;

final public class LoggingHttpRequestFilterTest {

    @Test
    public void assertFilter() {
        HttpRequestFilter httpRequestFilter = new LoggingHttpRequestFilter();
        httpRequestFilter.filter(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test"));
    }
}
