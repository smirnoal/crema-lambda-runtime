package com.smirnoal.lambda;

import java.util.function.Function;

public class LambdaHandlerBuilder<T, R> {
    Function<byte[], T> fromBytesToInput;
    Function<R, byte[]> fromOutputTypeToBytes;
    LambdaSerDe<T, R> lambdaSerDe;
    Function<T, R> handler;

    public LambdaHandlerBuilder<T, R> withInputTypeDeserializer(Function<byte[], T> func) {
        this.fromBytesToInput = func;
        return this;
    }

    public LambdaHandlerBuilder<T, R> withOutputTypeSerializer(Function<R, byte[]> func) {
        this.fromOutputTypeToBytes = func;
        return this;
    }

    public LambdaHandlerBuilder<T, R> withLambdaSerde(LambdaSerDe<T, R> lambdaSerDe) {
        this.lambdaSerDe = lambdaSerDe;
        return this;
    }

    public LambdaHandlerBuilder<T, R> withHandler(Function<T, R> func) {
        handler = func;
        return this;
    }

    public LambdaHandler<T, R> build() {
        return new LambdaHandler<T, R>() {
            @Override
            public R handle(T event) {
                return handler.apply(event);
            }

            @Override
            public T toInputType(byte[] bytes) {
                if (lambdaSerDe != null) {
                    return lambdaSerDe.getInputDeserializer().apply(bytes);
                }
                return fromBytesToInput.apply(bytes);
            }

            @Override
            public byte[] toBytes(R event) {
                if (lambdaSerDe != null) {
                    return lambdaSerDe.getOutputSerializer().apply(event);
                }
                return fromOutputTypeToBytes.apply(event);
            }
        };
    }
}
