package com.smirnoal.crema.serde;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.function.Function;

public final class GsonLambdaSerde {

    private GsonLambdaSerde() {
    }

    /**
     * Returns a {@link GsonBuilder} with standard type adapters (e.g. {@link Instant}) pre-registered.
     * Users can extend it with custom adapters before calling {@link GsonBuilder#create()}.
     *
     * <p>Example:
     * <pre>{@code
     * Gson gson = GsonLambdaSerde.defaultGsonBuilder()
     *     .registerTypeAdapter(MyType.class, myAdapter)
     *     .create();
     * }</pre>
     */
    public static GsonBuilder defaultGsonBuilder() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, StandardTypeAdapters.INSTANT);
    }

    /**
     * Returns a {@link Gson} configured with standard type adapters (e.g. {@link Instant}).
     * Equivalent to {@code defaultGsonBuilder().create()}.
     *
     * <p>To extend with custom adapters, use {@link #defaultGsonBuilder()} instead.
     */
    public static Gson defaultGson() {
        return defaultGsonBuilder().create();
    }

    public static <T, R> LambdaSerde<T, R> forTypes(Class<T> in, Class<R> out) {
        return forTypes(defaultGson(), in, out);
    }

    public static <T, R> LambdaSerde<T, R> forTypes(TypeToken<T> in, TypeToken<R> out) {
        return forTypes(defaultGson(), in, out);
    }

    public static <T, R> LambdaSerde<T, R> forTypes(Class<T> in, TypeToken<R> out) {
        return forTypes(defaultGson(), in, out);
    }

    public static <T, R> LambdaSerde<T, R> forTypes(TypeToken<T> in, Class<R> out) {
        return forTypes(defaultGson(), in, out);
    }

    public static <T, R> LambdaSerde<T, R> forTypes(Gson gson, Class<T> in, Class<R> out) {
        return new GsonLambdaSerdeImpl<>(gson, in, out);
    }

    public static <T, R> LambdaSerde<T, R> forTypes(Gson gson, TypeToken<T> in, TypeToken<R> out) {
        return new GsonLambdaSerdeImpl<>(gson, in.getType(), out.getType());
    }

    public static <T, R> LambdaSerde<T, R> forTypes(Gson gson, Class<T> in, TypeToken<R> out) {
        return new GsonLambdaSerdeImpl<>(gson, in, out.getType());
    }

    public static <T, R> LambdaSerde<T, R> forTypes(Gson gson, TypeToken<T> in, Class<R> out) {
        return new GsonLambdaSerdeImpl<>(gson, in.getType(), out);
    }

    private static final class GsonLambdaSerdeImpl<T, R> implements LambdaSerde<T, R> {
        private final Gson gson;
        private final Type inputType;
        private final Type outputType;

        GsonLambdaSerdeImpl(Gson gson, Type inputType, Type outputType) {
            this.gson = gson;
            this.inputType = inputType;
            this.outputType = outputType;
        }

        @Override
        public Function<byte[], T> inputDeserializer() {
            return bytes -> {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                String str = new String(bytes, StandardCharsets.UTF_8).trim();
                if (str.isEmpty() || "null".equals(str)) {
                    return null;
                }
                try {
                    return gson.fromJson(str, inputType);
                } catch (Exception e) {
                    throw new SerdeException("Failed to deserialize input", e);
                }
            };
        }

        @Override
        public Function<R, byte[]> outputSerializer() {
            return obj -> {
                if (outputType == Void.class || outputType == void.class) {
                    return new byte[0];
                }
                if (obj == null) {
                    return "null".getBytes(StandardCharsets.UTF_8);
                }
                try {
                    return gson.toJson(obj).getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    throw new SerdeException("Failed to serialize output", e);
                }
            };
        }
    }
}
