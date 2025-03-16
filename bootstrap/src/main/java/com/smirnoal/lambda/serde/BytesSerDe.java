package com.smirnoal.lambda.serde;

import java.util.function.Function;

public class BytesSerDe implements LambdaSerDe<byte[], byte[]> {

    @Override
    public Function<byte[], byte[]> inputDeserializer() {
        return Function.identity();
    }

    @Override
    public Function<byte[], byte[]> outputSerializer() {
        return Function.identity();
    }
}
