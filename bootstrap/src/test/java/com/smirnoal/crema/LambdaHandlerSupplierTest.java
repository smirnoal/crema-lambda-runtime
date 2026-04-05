package com.smirnoal.crema;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LambdaHandlerSupplierTest {

    @Test
    void supplierHandler_noInputDeserializerRequired() {
        LambdaHandler<Void, String> handler = new LambdaHandler<Void, String>()
                .withOutputTypeSerializer(s -> s.getBytes(StandardCharsets.UTF_8))
                .withHandler(() -> "ok");

        assertNull(handler.toInputType("{}".getBytes(StandardCharsets.UTF_8)));
        assertEquals("ok", handler.handle(null));
        assertEquals("ok", new String(handler.toBytes("ok"), StandardCharsets.UTF_8));
    }

    @Test
    void supplierHandler_methodReference() {
        LambdaHandler<Void, String> handler = new LambdaHandler<Void, String>()
                .withOutputTypeSerializer(s -> s.getBytes(StandardCharsets.UTF_8))
                .withHandler(this::supply);

        assertEquals("supplied", handler.handle(null));
        assertEquals("supplied", new String(handler.toBytes(handler.handle(null)), StandardCharsets.UTF_8));
    }

    private String supply() {
        return "supplied";
    }
}
