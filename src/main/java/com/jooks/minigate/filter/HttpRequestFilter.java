package com.jooks.minigate.filter;

import io.netty.handler.codec.http.HttpRequest;

public interface HttpRequestFilter {
    
    void filter(HttpRequest httpRequest);
    
}
