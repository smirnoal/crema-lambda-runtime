package com.smirnoal.crema.serde;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class StringSerde implements LambdaSerde<String, String> {

    @Override
    public Function<byte[], String> inputDeserializer() {
        return (bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    @Override
    public Function<String, byte[]> outputSerializer() {
        return (str -> str.getBytes(StandardCharsets.UTF_8));
    }
}
