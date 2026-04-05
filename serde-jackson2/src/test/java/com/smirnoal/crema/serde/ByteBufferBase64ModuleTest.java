package com.smirnoal.crema.serde;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ByteBufferBase64ModuleTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new ByteBufferBase64Module());

    @Test
    void deserializeBase64StringToByteBuffer() throws IOException {
        String json = "\"SGVsbG8=\"";
        ByteBuffer result = mapper.readValue(json, ByteBuffer.class);
        assertNotNull(result);
        String decoded = StandardCharsets.UTF_8.decode(result).toString();
        assertEquals("Hello", decoded);
    }

    @Test
    void serializeByteBufferToBase64String() throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap("Hello".getBytes(StandardCharsets.UTF_8));
        String json = mapper.writeValueAsString(buffer);
        assertEquals("\"SGVsbG8=\"", json);
    }

    @Test
    void roundTripByteBuffer() throws IOException {
        ByteBuffer original = ByteBuffer.wrap("Hello, this is a test 123.".getBytes(StandardCharsets.UTF_8));
        String json = mapper.writeValueAsString(original);
        ByteBuffer deserialized = mapper.readValue(json, ByteBuffer.class);
        assertNotNull(deserialized);
        assertEquals(StandardCharsets.UTF_8.decode(original).toString(),
                StandardCharsets.UTF_8.decode(deserialized).toString());
    }

    @Test
    void deserializeEmptyStringReturnsNull() throws IOException {
        String json = "\"\"";
        ByteBuffer result = mapper.readValue(json, ByteBuffer.class);
        assertNull(result);
    }

    @Test
    void serializeNullByteBuffer() throws IOException {
        String json = mapper.writeValueAsString((ByteBuffer) null);
        assertEquals("null", json);
    }

    @Test
    void deserializeNullReturnsNull() throws IOException {
        ByteBuffer result = mapper.readValue("null", ByteBuffer.class);
        assertNull(result);
    }

    @Test
    void listOfByteBuffersRoundTrip() throws IOException {
        List<ByteBuffer> original = List.of(
                ByteBuffer.wrap("a".getBytes(StandardCharsets.UTF_8)),
                ByteBuffer.wrap("bc".getBytes(StandardCharsets.UTF_8)));
        String json = mapper.writeValueAsString(original);
        List<ByteBuffer> deserialized = mapper.readValue(json, new TypeReference<List<ByteBuffer>>() {});
        assertNotNull(deserialized);
        assertEquals(2, deserialized.size());
        assertEquals("a", StandardCharsets.UTF_8.decode(deserialized.get(0)).toString());
        assertEquals("bc", StandardCharsets.UTF_8.decode(deserialized.get(1)).toString());
    }

}
