package com.smirnoal.lambda;

import com.smirnoal.lambda.serde.LambdaSerDe;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;

public class LambdaHandlerBuilder<T, R> {
    Function<byte[], T> fromBytesToInput;
    Function<R, byte[]> fromOutputTypeToBytes;
    Function<T, R> function;
    Consumer<T> consumer;
    Runnable runnable;

    public LambdaHandlerBuilder<T, R> withInputTypeDeserializer(Function<byte[], T> func) {
        this.fromBytesToInput = func;
        return this;
    }

    public LambdaHandlerBuilder<T, R> withOutputTypeSerializer(Function<R, byte[]> func) {
        this.fromOutputTypeToBytes = func;
        return this;
    }

    public LambdaHandlerBuilder<T, R> withLambdaSerde(LambdaSerDe<T, R> lambdaSerDe) {
        this.fromBytesToInput = lambdaSerDe.inputDeserializer();
        this.fromOutputTypeToBytes = lambdaSerDe.outputSerializer();
        return this;
    }

    public LambdaHandlerBuilder<T, R> withHandler(Function<T, R> function) {
        assertHandlerNotDefined();
        this.function = function;
        return this;
    }

    public LambdaHandlerBuilder<T, R> withHandler(Consumer<T> consumer) {
        assertHandlerNotDefined();
        this.consumer = consumer;
        return this;
    }

    public LambdaHandlerBuilder<T, R> withHandler(Runnable runnable) {
        assertHandlerNotDefined();
        this.runnable = runnable;
        return this;
    }

    private void assertHandlerNotDefined() {
        if (function != null) {
            throw new AssertionError("Function handler is already defined");
        } else if (consumer != null) {
            throw new AssertionError("Consumer handler is already defined");
        } else if (runnable != null) {
            throw new AssertionError("Runnable handler is already defined");
        }
    }

    public LambdaHandler<T, R> build() {
        return new LambdaHandler<>() {

            private static final byte[] EMTPY_BYTE_ARRAY = new byte[0];
            private static final byte[] NULL_RESPONSE = "null".getBytes(StandardCharsets.UTF_8);

            @Override
            public R handle(T event) {
                if (function != null) {
                    return function.apply(event);
                } else if (consumer != null) {
                    consumer.accept(event);
                    return null;
                } else if (runnable != null) {
                    runnable.run();
                    return null;
                }
                throw new RuntimeException("Handler is not defined");
            }

            @Override
            public T toInputType(byte[] bytes) {
                if (function == null && consumer == null) {
                    return null;
                }
                return fromBytesToInput.apply(bytes);
            }

            @Override
            public byte[] toBytes(R event) {
                if (function == null) {
                    return EMTPY_BYTE_ARRAY;
                }
                if (event == null) {
                    return NULL_RESPONSE;
                }
                return fromOutputTypeToBytes.apply(event);
            }
        };
    }
}
