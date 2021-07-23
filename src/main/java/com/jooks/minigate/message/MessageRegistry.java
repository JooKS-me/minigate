package com.jooks.minigate.message;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息注册中心
 */
public class MessageRegistry {

    private static final MessageRegistry instance = new MessageRegistry();

    private static final ConcurrentHashMap<Object, Object> map = new ConcurrentHashMap<>();

    private MessageRegistry() {
    }

    public static MessageRegistry getInstance() {
        return instance;
    }

    public void put(Object key, Object value) {
        map.put(key, value);
    }

    public Object get(Object key) {
        return map.get(key);
    }
}
