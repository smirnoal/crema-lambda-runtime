package com.smirnoal.lambda.rapid.client.serde;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smirnoal.lambda.rapid.client.dto.ErrorRequest;
import com.smirnoal.lambda.rapid.client.dto.XRayErrorCause;

public class PayloadSerializers {
    public static byte[] serialize(ErrorRequest error) {
        return SingletonHelper.toBytes(error);
    }

    public static byte[] serialize(XRayErrorCause xRayErrorCause) {
        return SingletonHelper.toBytes(xRayErrorCause);
    }

    private static class SingletonHelper {
            final static Gson GSON = new GsonBuilder().create();

            public static byte[] toBytes(Object obj) {
                return GSON.toJson(obj).getBytes();
            }
    }
}
