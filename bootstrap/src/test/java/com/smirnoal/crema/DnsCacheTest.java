package com.smirnoal.crema;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DnsCache}. Per design, the clearer is best-effort and must not throw.
 *
 * @see <a href="https://github.com/alibaba/java-dns-cache-manipulator">Alibaba DCM</a> for reference implementation
 */
class DnsCacheTest {

    @Test
    void clearInetAddressCache_completesWithoutThrowing() {
        assertDoesNotThrow(DnsCache::clearInetAddressCache);
    }

    @Test
    void clearInetAddressCache_clearsPopulatedCache() throws Exception {
        InetAddress.getByName("example.com");
        int sizeBefore = getCacheSize();
        assertTrue(sizeBefore > 0, "Cache should have entries after resolve");

        DnsCache.clearInetAddressCache();

        int sizeAfter = getCacheSize();
        assertEquals(0, sizeAfter, "Cache should be empty after clear");
    }

    /**
     * Returns cache size via reflection. JDK 17+: InetAddress has cache (ConcurrentMap).
     */
    private static int getCacheSize() throws ReflectiveOperationException {
        Field f = InetAddress.class.getDeclaredField("cache");
        f.setAccessible(true);
        Object val = f.get(null);
        if (val instanceof Map<?, ?> map) {
            return map.size();
        }
        throw new IllegalStateException("InetAddress.cache is not a Map");
    }
}
