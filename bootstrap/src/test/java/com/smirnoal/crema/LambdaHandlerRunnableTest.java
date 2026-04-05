package com.smirnoal.crema;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LambdaHandlerRunnableTest {

    @Test
    void runnableHandler_returnsNullAndEmptyBytes() {
        LambdaHandler<Void, Void> handler = new LambdaHandler<Void, Void>()
                .withHandler(() -> {});

        assertNull(handler.handle(null));
        assertArrayEquals(new byte[0], handler.toBytes(null));
    }

    @Test
    void runnableHandler_executes() {
        AtomicInteger count = new AtomicInteger(0);
        LambdaHandler<Void, Void> handler = new LambdaHandler<Void, Void>()
                .withHandler(count::incrementAndGet);

        handler.handle(null);
        handler.handle(null);
        assertEquals(2, count.get());
    }

    @Test
    void runnableHandler_toInputTypeReturnsNull() {
        LambdaHandler<Void, Void> handler = new LambdaHandler<Void, Void>()
                .withHandler(() -> {});
        assertNull(handler.toInputType("any".getBytes()));
    }
}
