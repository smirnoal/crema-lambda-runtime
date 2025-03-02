package com.smirnoal.lambda;

public interface Serializer<T> {
    byte[] toBytes(T object);
}