package com.smirnoal.rapid.client.serde;

public interface Serializer {
    byte[] toBytes(Object obj);
}
