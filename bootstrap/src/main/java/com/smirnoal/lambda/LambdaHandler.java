package com.smirnoal.lambda;

public interface LambdaHandler<T, R> {
    R handle(T event);

    byte[] toBytes(R object);

    T toInputType(byte[] bytes);
}
