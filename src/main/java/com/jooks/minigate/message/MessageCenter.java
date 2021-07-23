package com.jooks.minigate.message;

import com.google.common.eventbus.AsyncEventBus;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 消息处理中心
 */
public class MessageCenter {

    private static final MessageCenter instance = new MessageCenter();

    private static final AsyncEventBus asyncEventBus = new AsyncEventBus(new ThreadPoolExecutor(2, 8, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>()));

    private MessageCenter() {
    }

    public static MessageCenter getInstance() {
        return instance;
    }

    public AsyncEventBus getAsyncEventBus() {
        return asyncEventBus;
    }
}
