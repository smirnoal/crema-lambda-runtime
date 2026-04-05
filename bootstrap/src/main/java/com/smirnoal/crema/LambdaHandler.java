package com.smirnoal.crema;

import com.smirnoal.crema.serde.LambdaSerde;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LambdaHandler<T, R> {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final byte[] NULL_RESPONSE = "null".getBytes(StandardCharsets.UTF_8);

    private Function<byte[], T> fromBytesToInput;
    private Function<R, byte[]> fromOutputTypeToBytes;
    private Function<T, R> function;
    private Consumer<T> consumer;
    private Runnable runnable;
    private Supplier<R> supplier;

    public LambdaHandler<T, R> withInputTypeDeserializer(Function<byte[], T> func) {
        this.fromBytesToInput = func;
        return this;
    }

    public LambdaHandler<T, R> withOutputTypeSerializer(Function<R, byte[]> func) {
        this.fromOutputTypeToBytes = func;
        return this;
    }

    public LambdaHandler<T, R> withLambdaSerde(LambdaSerde<T, R> lambdaSerDe) {
        this.fromBytesToInput = lambdaSerDe.inputDeserializer();
        this.fromOutputTypeToBytes = lambdaSerDe.outputSerializer();
        return this;
    }

    public LambdaHandler<T, R> withHandler(Function<T, R> function) {
        assertHandlerNotDefined();
        this.function = function;
        return this;
    }

    public LambdaHandler<T, R> withHandler(Consumer<T> consumer) {
        assertHandlerNotDefined();
        this.consumer = consumer;
        return this;
    }

    public LambdaHandler<T, R> withHandler(Runnable runnable) {
        assertHandlerNotDefined();
        this.runnable = runnable;
        return this;
    }

    public LambdaHandler<T, R> withHandler(Supplier<R> supplier) {
        assertHandlerNotDefined();
        this.supplier = supplier;
        return this;
    }

    private void assertHandlerNotDefined() {
        if (function != null) {
            throw new AssertionError("Function handler is already defined");
        } else if (consumer != null) {
            throw new AssertionError("Consumer handler is already defined");
        } else if (runnable != null) {
            throw new AssertionError("Runnable handler is already defined");
        } else if (supplier != null) {
            throw new AssertionError("Supplier handler is already defined");
        }
    }

    R handle(T event) {
        if (supplier != null) {
            return supplier.get();
        } else if (function != null) {
            return function.apply(event);
        } else if (consumer != null) {
            consumer.accept(event);
            return null;
        } else if (runnable != null) {
            runnable.run();
            return null;
        }
        throw new AssertionError("Handler is not defined");
    }

    T toInputType(byte[] bytes) {
        if (supplier != null || (function == null && consumer == null)) {
            return null;
        }
        return fromBytesToInput.apply(bytes);
    }

    byte[] toBytes(R event) {
        if (function == null && supplier == null) {
            return EMPTY_BYTE_ARRAY;
        }
        if (event == null) {
            return NULL_RESPONSE;
        }
        return fromOutputTypeToBytes.apply(event);
    }
}
