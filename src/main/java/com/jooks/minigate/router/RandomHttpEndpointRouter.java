package com.jooks.minigate.router;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class RandomHttpEndpointRouter implements HttpEndpointRouter {

    private static AtomicInteger atomicInteger = new AtomicInteger();

    /**
     * 以时间为种子，进行随机路由
     * @param urls urls
     * @return url
     */
    @Override
    public String routeByRandom(List<String> urls) {
        int size = urls.size();
        Random random = new Random(System.currentTimeMillis());
        return urls.get(random.nextInt(size));
    }

    /**
     * 不加权轮询算法
     * @param urls urls
     * @return url
     */
    @Override
    public String routeByRoundRobin(List<String> urls) {
        int index = atomicInteger.getAndIncrement() % urls.size();
        return urls.get(index);
    }
}
