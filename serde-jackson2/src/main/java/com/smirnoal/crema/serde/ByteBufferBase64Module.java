package com.smirnoal.crema.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.Serial;
import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * Jackson module that serializes/deserializes {@link ByteBuffer} as base64-encoded JSON strings.
 * Used for AWS Lambda event binary fields (SQS messageAttributes, Kinesis data, DynamoDB AttributeValue B/BS).
 */
public final class ByteBufferBase64Module extends SimpleModule {

    @Serial
    private static final long serialVersionUID = 1L;

    public ByteBufferBase64Module() {
        addSerializer(ByteBuffer.class, new ByteBufferBase64Serializer());
        addDeserializer(ByteBuffer.class, new ByteBufferBase64Deserializer());
    }

    private static final class ByteBufferBase64Serializer extends JsonSerializer<ByteBuffer> {
        @Override
        public void serialize(ByteBuffer value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            byte[] bytes;
            int pos = value.position();
            int limit = value.limit();
            if (value.hasArray() && value.arrayOffset() == 0 && pos == 0 && limit == value.capacity()) {
                bytes = value.array();
            } else {
                bytes = new byte[value.remaining()];
                value.duplicate().get(bytes);
            }
            gen.writeString(Base64.getEncoder().encodeToString(bytes));
        }
    }

    private static final class ByteBufferBase64Deserializer extends JsonDeserializer<ByteBuffer> {
        @Override
        public ByteBuffer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String base64 = p.getText();
            if (base64 == null || base64.isEmpty()) {
                return null;
            }
            byte[] decoded = Base64.getDecoder().decode(base64);
            return ByteBuffer.wrap(decoded);
        }
    }
}
