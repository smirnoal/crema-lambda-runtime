package com.smirnoal.lambda;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Map;

final class DnsCache {
    private DnsCache() {
    }

    static void clearInetAddressCache() {
        clearStaticMapField("cache");
        clearStaticMapField("expirySet");
    }

    private static void clearStaticMapField(String fieldName) {
        try {
            Field field = InetAddress.class.getDeclaredField(fieldName);
            Object value = readStaticField(field);
            if (value instanceof Map<?, ?> map) {
                map.clear();
            }
        } catch (Throwable ignored) {
            // Best effort only
        }
    }

    private static Object readStaticField(Field field) throws ReflectiveOperationException {
        field.setAccessible(true);
        return field.get(null);
    }
}
