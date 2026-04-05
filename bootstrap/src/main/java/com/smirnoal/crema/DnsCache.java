package com.smirnoal.crema;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;

final class DnsCache {
    private DnsCache() {
    }

    /**
     * Clears InetAddress DNS cache. Best-effort: suppresses reflection errors.
     * JDK 17+: clears cache (ConcurrentMap) and expirySet (NavigableSet).
     */
    static void clearInetAddressCache() {
        clearStaticMapField("cache");
        clearStaticMapField("expirySet");
    }

    private static void clearStaticMapField(String fieldName) {
        try {
            Field field = InetAddress.class.getDeclaredField(fieldName);
            Object value = readStaticField(field);
            clearIfMapOrCollection(value);
        } catch (Throwable ignored) {
            // Best effort only
        }
    }

    private static void clearIfMapOrCollection(Object value) {
        if (value instanceof Map<?, ?> map) {
            map.clear();
        } else if (value instanceof Collection<?> col) {
            col.clear();
        }
    }

    private static Object readStaticField(Field field) throws ReflectiveOperationException {
        field.setAccessible(true);
        return field.get(null);
    }
}
