package com.smirnoal.crema.log;

import com.smirnoal.crema.log.RicLog.RicLogger;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
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
        log.message(() -> {
            callCount.incrementAndGet();
            return "should not run";
        });
        assertEquals(0, callCount.get());
    }

    @Test
    void exception_doesNotThrowWhenDisabled() {
        RicLogger log = RicLog.getLogger("never-enabled-" + System.nanoTime());
        Exception e = new RuntimeException("test");
        log.exception("message", e);
        log.exception(e);
    }

    @Test
    void whenDisabled_exceptionIsNeverFormatted() {
        RicLogger log = RicLog.getLogger("never-enabled-" + System.nanoTime());
        AtomicInteger formatCount = new AtomicInteger(0);
        var traceable = new RuntimeException("test") {
            @Override
            public void printStackTrace(PrintWriter s) {
                formatCount.incrementAndGet();
                super.printStackTrace(s);
            }
        };
        log.exception("message", traceable);
        log.exception(traceable);
        assertEquals(0, formatCount.get());
    }

    @Test
    void whenEnabled_exceptionIsFormatted() {
        RicLogger log = RicLog.getLogger("riclog-enabled-test");
        assertTrue(log.isEnabled());
        AtomicInteger formatCount = new AtomicInteger(0);
        var traceable = new RuntimeException("test") {
            @Override
            public void printStackTrace(PrintWriter s) {
                formatCount.incrementAndGet();
                super.printStackTrace(s);
            }
        };
        log.exception("message", traceable);
        log.exception(traceable);
        assertEquals(2, formatCount.get());
    }
}
