package com.smirnoal.lambda.rapid.client.serde;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smirnoal.lambda.Serializer;
import com.smirnoal.lambda.rapid.client.dto.ErrorRequest;
import com.smirnoal.lambda.rapid.client.dto.XRayErrorCause;

public class PayloadSerializers {
    public static byte[] serialize(ErrorRequest error) {
        return SingletonHelper.SERIALIZER.toBytes(error);
    }

    public static byte[] serialize(XRayErrorCause xRayErrorCause) {
        return SingletonHelper.SERIALIZER.toBytes(xRayErrorCause);
    }

    private static class SingletonHelper {
        private static final Serializer SERIALIZER = new Serializer() {
            final Gson GSON = new GsonBuilder().create();

            @Override
            public byte[] toBytes(Object obj) {
                return GSON.toJson(obj).getBytes();
            }
        };
    }
}
