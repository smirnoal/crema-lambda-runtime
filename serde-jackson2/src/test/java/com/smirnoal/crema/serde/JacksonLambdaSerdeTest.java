package com.smirnoal.crema.serde;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JacksonLambdaSerdeTest {

    record TestPojo(String name, int count) {
    }

    record NestedPojo(String id, TestPojo inner) {
    }

    private final ObjectMapper defaultMapper = new ObjectMapper();
    private final ObjectMapper mapperWithJavaTime = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void roundTripPojo() {
        LambdaSerde<TestPojo, TestPojo> serde = JacksonLambdaSerde.forTypes(defaultMapper, TestPojo.class, TestPojo.class);
        TestPojo original = new TestPojo("test", 42);
        byte[] serialized = serde.outputSerializer().apply(original);
        TestPojo deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void roundTripNestedTypes() {
        LambdaSerde<NestedPojo, NestedPojo> serde = JacksonLambdaSerde.forTypes(defaultMapper, NestedPojo.class, NestedPojo.class);
        NestedPojo original = new NestedPojo("id-1", new TestPojo("inner", 10));
        byte[] serialized = serde.outputSerializer().apply(original);
        NestedPojo deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void roundTripWithTypeReference() {
        var listType = new TypeReference<List<TestPojo>>() {
        };
        LambdaSerde<List<TestPojo>, List<TestPojo>> serde = JacksonLambdaSerde.forTypes(defaultMapper, listType, listType);
        List<TestPojo> original = List.of(new TestPojo("a", 1), new TestPojo("b", 2));
        byte[] serialized = serde.outputSerializer().apply(original);
        List<TestPojo> deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void emptyInputReturnsNull() {
        LambdaSerde<TestPojo, TestPojo> serde = JacksonLambdaSerde.forTypes(defaultMapper, TestPojo.class, TestPojo.class);
        assertNull(serde.inputDeserializer().apply(new byte[0]));
        assertNull(serde.inputDeserializer().apply("null".getBytes()));
    }

    @Test
    void nullOutput() {
        LambdaSerde<TestPojo, Void> serde = JacksonLambdaSerde.forTypes(defaultMapper, TestPojo.class, Void.class);
        byte[] result = serde.outputSerializer().apply(null);
        assertEquals(0, result.length);

        LambdaSerde<String, String> strSerde = JacksonLambdaSerde.forTypes(defaultMapper, String.class, String.class);
        byte[] nullStr = strSerde.outputSerializer().apply(null);
        assertArrayEquals("null".getBytes(), nullStr);
    }

    @Test
    void customObjectMapper() {
        record WithInstant(Instant at) {
        }
        LambdaSerde<WithInstant, WithInstant> serde = JacksonLambdaSerde.forTypes(mapperWithJavaTime, WithInstant.class, WithInstant.class);
        WithInstant original = new WithInstant(Instant.parse("2025-03-12T10:30:00Z"));
        byte[] serialized = serde.outputSerializer().apply(original);
        WithInstant deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void forTypesWithDefaultSupportsInstant() {
        record WithInstant(Instant at) {
        }
        LambdaSerde<WithInstant, WithInstant> serde = JacksonLambdaSerde.forTypes(WithInstant.class, WithInstant.class);
        WithInstant original = new WithInstant(Instant.parse("2025-03-12T10:30:00Z"));
        byte[] serialized = serde.outputSerializer().apply(original);
        WithInstant deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void forTypesWithTypeReferenceUsesDefaultObjectMapper() {
        record WithInstant(Instant at) {
        }
        var listType = new TypeReference<List<WithInstant>>() {
        };
        LambdaSerde<List<WithInstant>, List<WithInstant>> serde = JacksonLambdaSerde.forTypes(listType, listType);
        List<WithInstant> original = List.of(
                new WithInstant(Instant.parse("2025-03-12T10:30:00Z")),
                new WithInstant(Instant.parse("2025-03-12T11:00:00Z")));
        byte[] serialized = serde.outputSerializer().apply(original);
        List<WithInstant> deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void defaultObjectMapperBuilderCanBeExtended() {
        ObjectMapper mapper = JacksonLambdaSerde.defaultObjectMapperBuilder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
        LambdaSerde<TestPojo, TestPojo> serde = JacksonLambdaSerde.forTypes(mapper, TestPojo.class, TestPojo.class);
        TestPojo original = new TestPojo("test", 42);
        byte[] serialized = serde.outputSerializer().apply(original);
        TestPojo deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void forTypesClassTypeReference() throws IOException {
        var listType = new TypeReference<List<TestPojo>>() {
        };
        LambdaSerde<TestPojo, List<TestPojo>> serde = JacksonLambdaSerde.forTypes(defaultMapper, TestPojo.class, listType);
        TestPojo original = new TestPojo("test", 42);
        byte[] inputBytes = defaultMapper.writeValueAsBytes(original);
        TestPojo deserialized = serde.inputDeserializer().apply(inputBytes);
        assertEquals(original, deserialized);

        List<TestPojo> list = List.of(original);
        byte[] outputBytes = serde.outputSerializer().apply(list);
        List<TestPojo> parsed = defaultMapper.readValue(outputBytes, listType);
        assertEquals(list, parsed);
    }

    @Test
    void forTypesTypeReferenceClass() throws IOException {
        var listType = new TypeReference<List<TestPojo>>() {
        };
        LambdaSerde<List<TestPojo>, TestPojo> serde = JacksonLambdaSerde.forTypes(defaultMapper, listType, TestPojo.class);
        List<TestPojo> original = List.of(new TestPojo("a", 1), new TestPojo("b", 2));
        byte[] inputBytes = defaultMapper.writeValueAsBytes(original);
        List<TestPojo> deserialized = serde.inputDeserializer().apply(inputBytes);
        assertEquals(original, deserialized);

        TestPojo first = original.get(0);
        byte[] outputBytes = serde.outputSerializer().apply(first);
        TestPojo parsed = defaultMapper.readValue(outputBytes, TestPojo.class);
        assertEquals(first, parsed);
    }
}
