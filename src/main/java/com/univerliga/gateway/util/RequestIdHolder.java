package com.univerliga.gateway.util;

public final class RequestIdHolder {
    public static final String ATTRIBUTE_NAME = "requestId";
    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private RequestIdHolder() {
    }

    public static void set(String requestId) {
        HOLDER.set(requestId);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
