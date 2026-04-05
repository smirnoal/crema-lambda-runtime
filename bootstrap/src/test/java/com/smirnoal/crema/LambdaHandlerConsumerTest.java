package com.smirnoal.crema;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LambdaHandlerConsumerTest {

    @Test
    void consumerHandler_acceptsInputReturnsNull() {
        AtomicReference<String> received = new AtomicReference<>();
        LambdaHandler<String, Void> handler = new LambdaHandler<String, Void>()
                .withInputTypeDeserializer(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .withHandler(received::set);

        handler.handle("hello");
        assertEquals("hello", received.get());
        assertNull(handler.handle("world"));
        assertEquals("world", received.get());
    }

    @Test
    void consumerHandler_toBytesReturnsEmpty() {
        LambdaHandler<String, Void> handler = new LambdaHandler<String, Void>()
                .withInputTypeDeserializer(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .withHandler(s -> {});
        assertArrayEquals(new byte[0], handler.toBytes(null));
    }

    @Test
    void consumerHandler_toInputTypeDeserializes() {
        AtomicReference<String> received = new AtomicReference<>();
        LambdaHandler<String, Void> handler = new LambdaHandler<String, Void>()
                .withInputTypeDeserializer(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .withHandler(received::set);

        String input = handler.toInputType("consumed".getBytes(StandardCharsets.UTF_8));
        assertEquals("consumed", input);
        handler.handle(input);
        assertEquals("consumed", received.get());
    }
}
