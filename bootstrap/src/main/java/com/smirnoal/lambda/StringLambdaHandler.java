package com.smirnoal.lambda;

import java.nio.charset.StandardCharsets;

public abstract class StringLambdaHandler implements LambdaHandler<String, String> {

    @Override
    public byte[] toBytes(String object) {
        return object.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toInputType(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
