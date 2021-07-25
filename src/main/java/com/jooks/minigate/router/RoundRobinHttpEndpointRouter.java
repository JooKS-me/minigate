package com.jooks.minigate.router;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinHttpEndpointRouter implements HttpEndpointRouter{

    private static AtomicInteger atomicInteger = new AtomicInteger();

    /**
     * 不加权轮询算法
     * @param urls urls
     * @return url
     */
    @Override
    public String route(List<String> urls) {
        int index = atomicInteger.getAndIncrement() % urls.size();
        return urls.get(index);
    }
}
