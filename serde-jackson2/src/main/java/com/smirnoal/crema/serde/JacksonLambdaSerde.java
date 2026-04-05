package com.smirnoal.crema.serde;

import com.smirnoal.crema.log.RicLog;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public final class JacksonLambdaSerde {

    private JacksonLambdaSerde() {
    }

    /**
     * Returns an {@link ObjectMapper} builder with standard modules (e.g. {@link JavaTimeModule} for {@link java.time} types)
     * and {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} disabled for forward compatibility with evolving payloads.
     * Users can extend it with custom modules before calling {@code build()}.
     *
     * <p>Example:
     * <pre>{@code
     * ObjectMapper mapper = JacksonLambdaSerde.defaultObjectMapperBuilder()
     *     .addModule(myCustomModule)
     *     .build();
     * }</pre>
     */
    public static JsonMapper.Builder defaultObjectMapperBuilder() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = defaultObjectMapperBuilder().build();

    /**
     * Returns a shared {@link ObjectMapper} configured with standard modules (e.g. {@link JavaTimeModule})
     * and unknown-property-tolerant deserialization.
     *
     * <p>To customize configuration, use {@link #defaultObjectMapperBuilder()} and pass the resulting mapper to
     * {@link #forTypes(ObjectMapper, Class, Class)} (or overloads).
     */
    public static ObjectMapper defaultObjectMapper() {
        return DEFAULT_OBJECT_MAPPER;
    }

    public static <T, R> LambdaSerde<T, R> forTypes(Class<T> in, Class<R> out) {
        return forTypes(DEFAULT_OBJECT_MAPPER, in, out);
    }

    public static <T, R> LambdaSerde<T, R> forTypes(TypeReference<T> in, TypeReference<R> out) {
        return forTypes(DEFAULT_OBJECT_MAPPER, in, out);
    }

    public static <T, R> LambdaSerde<T, R> forTypes(Class<T> in, TypeReference<R> out) {
        return forTypes(DEFAULT_OBJECT_MAPPER, in, out);
    }

    public static <T, R> LambdaSerde<T, R> forTypes(TypeReference<T> in, Class<R> out) {
        return forTypes(DEFAULT_OBJECT_MAPPER, in, out);
    }

    public static <T, R> LambdaSerde<T, R> forTypes(ObjectMapper mapper, Class<T> in, Class<R> out) {
        return new JacksonLambdaSerdeImpl<>(mapper, mapper.constructType(in), mapper.constructType(out));
    }

    public static <T, R> LambdaSerde<T, R> forTypes(ObjectMapper mapper, TypeReference<T> in, TypeReference<R> out) {
        return new JacksonLambdaSerdeImpl<>(
                mapper,
                mapper.getTypeFactory().constructType(in),
                mapper.getTypeFactory().constructType(out)
        );
    }

    public static <T, R> LambdaSerde<T, R> forTypes(ObjectMapper mapper, Class<T> in, TypeReference<R> out) {
        return new JacksonLambdaSerdeImpl<>(
                mapper,
                mapper.constructType(in),
                mapper.getTypeFactory().constructType(out)
        );
    }

    public static <T, R> LambdaSerde<T, R> forTypes(ObjectMapper mapper, TypeReference<T> in, Class<R> out) {
        return new JacksonLambdaSerdeImpl<>(
                mapper,
                mapper.getTypeFactory().constructType(in),
                mapper.constructType(out)
        );
    }

    private static final class JacksonLambdaSerdeImpl<T, R> implements LambdaSerde<T, R> {
        private static final RicLog.RicLogger log = RicLog.getLogger("serde");

        private final ObjectMapper mapper;
        private final JavaType inputType;
        private final JavaType outputType;

        JacksonLambdaSerdeImpl(ObjectMapper mapper, JavaType inputType, JavaType outputType) {
            this.mapper = mapper;
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
                    return mapper.readValue(bytes, inputType);
                } catch (Exception e) {
                    log.exception("Failed to deserialize " + inputType.getTypeName() + " inputLen=" + bytes.length, e);
                    throw new SerdeException("Failed to deserialize input", e);
                }
            };
        }

        @Override
        public Function<R, byte[]> outputSerializer() {
            return obj -> {
                if (outputType.getRawClass() == Void.class) {
                    return new byte[0];
                }
                if (obj == null) {
                    return "null".getBytes(StandardCharsets.UTF_8);
                }
                try {
                    return mapper.writeValueAsBytes(obj);
                } catch (Exception e) {
                    log.exception("Failed to serialize " + outputType.getTypeName(), e);
                    throw new SerdeException("Failed to serialize output", e);
                }
            };
        }
    }
}
