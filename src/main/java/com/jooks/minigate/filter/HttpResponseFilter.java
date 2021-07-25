package com.jooks.minigate.filter;

import io.netty.handler.codec.http.HttpResponse;

public interface HttpResponseFilter {

    void filter(HttpResponse response);

}
