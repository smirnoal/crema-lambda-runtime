package com.smirnoal.crema;

import com.smirnoal.crema.serde.StringSerde;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LambdaHandlerFunctionTest {

    @Test
    void functionHandler_transformsInputToOutput() {
        LambdaHandler<String, String> handler = new LambdaHandler<String, String>()
                .withLambdaSerde(new StringSerde())
                .withHandler(s -> "echo: " + s);

        String input = "hello";
        assertEquals("echo: hello", handler.handle(input));
        assertArrayEquals("echo: hello".getBytes(StandardCharsets.UTF_8), handler.toBytes("echo: hello"));
    }

    @Test
    void functionHandler_toInputTypeDeserializes() {
        LambdaHandler<String, Integer> handler = new LambdaHandler<String, Integer>()
                .withInputTypeDeserializer(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .withOutputTypeSerializer(i -> String.valueOf(i).getBytes(StandardCharsets.UTF_8))
                .withHandler((Function<String, Integer>) Integer::parseInt);

        String deserialized = handler.toInputType("42".getBytes(StandardCharsets.UTF_8));
        assertEquals("42", deserialized);
        assertEquals(42, handler.handle(deserialized));
    }
}
