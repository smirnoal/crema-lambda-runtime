package com.smirnoal.crema.serde;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GsonLambdaSerdeTest {

    record TestPojo(String name, int count) {
    }

    record NestedPojo(String id, TestPojo inner) {
    }

    private final Gson defaultGson = new Gson();

    @Test
    void roundTripPojo() {
        LambdaSerde<TestPojo, TestPojo> serde = GsonLambdaSerde.forTypes(defaultGson, TestPojo.class, TestPojo.class);
        TestPojo original = new TestPojo("test", 42);
        byte[] serialized = serde.outputSerializer().apply(original);
        TestPojo deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void roundTripNestedTypes() {
        LambdaSerde<NestedPojo, NestedPojo> serde = GsonLambdaSerde.forTypes(defaultGson, NestedPojo.class, NestedPojo.class);
        NestedPojo original = new NestedPojo("id-1", new TestPojo("inner", 10));
        byte[] serialized = serde.outputSerializer().apply(original);
        NestedPojo deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void roundTripWithTypeReference() {
        TypeToken<List<TestPojo>> listType = new TypeToken<>() {
        };
        LambdaSerde<List<TestPojo>, List<TestPojo>> serde = GsonLambdaSerde.forTypes(defaultGson, listType, listType);
        List<TestPojo> original = List.of(new TestPojo("a", 1), new TestPojo("b", 2));
        byte[] serialized = serde.outputSerializer().apply(original);
        List<TestPojo> deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void roundTripMapWithTypeToken() {
        TypeToken<Map<String, TestPojo>> mapType = new TypeToken<>() {
        };
        LambdaSerde<Map<String, TestPojo>, Map<String, TestPojo>> serde = GsonLambdaSerde.forTypes(defaultGson, mapType, mapType);
        Map<String, TestPojo> original = Map.of("a", new TestPojo("a", 1), "b", new TestPojo("b", 2));
        byte[] serialized = serde.outputSerializer().apply(original);
        Map<String, TestPojo> deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void emptyInputReturnsNull() {
        LambdaSerde<TestPojo, TestPojo> serde = GsonLambdaSerde.forTypes(defaultGson, TestPojo.class, TestPojo.class);
        assertNull(serde.inputDeserializer().apply(new byte[0]));
        assertNull(serde.inputDeserializer().apply("null".getBytes()));
    }

    @Test
    void nullOutput() {
        LambdaSerde<TestPojo, Void> serde = GsonLambdaSerde.forTypes(defaultGson, TestPojo.class, Void.class);
        byte[] result = serde.outputSerializer().apply(null);
        assertEquals(0, result.length);

        LambdaSerde<String, String> strSerde = GsonLambdaSerde.forTypes(defaultGson, String.class, String.class);
        byte[] nullStr = strSerde.outputSerializer().apply(null);
        assertArrayEquals("null".getBytes(), nullStr);
    }

    @Test
    void forTypesWithDefaultSupportsInstant() {
        record WithInstant(Instant at) {
        }
        LambdaSerde<WithInstant, WithInstant> serde = GsonLambdaSerde.forTypes(WithInstant.class, WithInstant.class);
        WithInstant original = new WithInstant(Instant.parse("2025-03-12T10:30:00Z"));
        byte[] serialized = serde.outputSerializer().apply(original);
        WithInstant deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void forTypesWithTypeTokenUsesDefaultGson() {
        record WithInstant(Instant at) {
        }
        TypeToken<List<WithInstant>> listType = new TypeToken<>() {
        };
        LambdaSerde<List<WithInstant>, List<WithInstant>> serde = GsonLambdaSerde.forTypes(listType, listType);
        List<WithInstant> original = List.of(
                new WithInstant(Instant.parse("2025-03-12T10:30:00Z")),
                new WithInstant(Instant.parse("2025-03-12T11:00:00Z")));
        byte[] serialized = serde.outputSerializer().apply(original);
        List<WithInstant> deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void defaultGsonBuilderCanBeExtended() {
        Gson gson = GsonLambdaSerde.defaultGsonBuilder()
                .serializeNulls()
                .create();
        LambdaSerde<TestPojo, TestPojo> serde = GsonLambdaSerde.forTypes(gson, TestPojo.class, TestPojo.class);
        TestPojo original = new TestPojo("test", 42);
        byte[] serialized = serde.outputSerializer().apply(original);
        TestPojo deserialized = serde.inputDeserializer().apply(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    void forTypesClassTypeToken() {
        TypeToken<List<TestPojo>> listType = new TypeToken<>() {
        };
        LambdaSerde<TestPojo, List<TestPojo>> serde = GsonLambdaSerde.forTypes(defaultGson, TestPojo.class, listType);
        TestPojo original = new TestPojo("test", 42);
        byte[] inputBytes = defaultGson.toJson(original).getBytes(StandardCharsets.UTF_8);
        TestPojo deserialized = serde.inputDeserializer().apply(inputBytes);
        assertEquals(original, deserialized);

        List<TestPojo> list = List.of(original);
        byte[] outputBytes = serde.outputSerializer().apply(list);
        List<TestPojo> parsed = defaultGson.fromJson(new String(outputBytes, StandardCharsets.UTF_8), listType);
        assertEquals(list, parsed);
    }

    @Test
    void forTypesTypeTokenClass() {
        TypeToken<List<TestPojo>> listType = new TypeToken<>() {
        };
        LambdaSerde<List<TestPojo>, TestPojo> serde = GsonLambdaSerde.forTypes(defaultGson, listType, TestPojo.class);
        List<TestPojo> original = List.of(new TestPojo("a", 1), new TestPojo("b", 2));
        byte[] inputBytes = defaultGson.toJson(original).getBytes(StandardCharsets.UTF_8);
        List<TestPojo> deserialized = serde.inputDeserializer().apply(inputBytes);
        assertEquals(original, deserialized);

        TestPojo first = original.get(0);
        byte[] outputBytes = serde.outputSerializer().apply(first);
        TestPojo parsed = defaultGson.fromJson(new String(outputBytes, StandardCharsets.UTF_8), TestPojo.class);
        assertEquals(first, parsed);
    }
}
