package com.smirnoal.lambda.log;

import com.smirnoal.lambda.log.RicLog.RicLogger;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RicLogTest {

    @Test
    void getLoggerReturnsCachedInstanceForSameName() {
        RicLogger a = RicLog.getLogger("cached-test");
        RicLogger b = RicLog.getLogger("cached-test");
        assertSame(a, b);
    }

    @Test
    void getLoggerReturnsDifferentInstancesForDifferentNames() {
        RicLogger a = RicLog.getLogger("cat-a");
        RicLogger b = RicLog.getLogger("cat-b");
        assertNotSame(a, b);
    }

    @Test
    void whenDisabled_supplierIsNotInvoked() {
        RicLogger log = RicLog.getLogger("never-enabled-category-" + System.nanoTime());
        AtomicInteger callCount = new AtomicInteger(0);
        log.log(() -> {
            callCount.incrementAndGet();
            return "should not run";
        });
        assertEquals(0, callCount.get());
    }
}
