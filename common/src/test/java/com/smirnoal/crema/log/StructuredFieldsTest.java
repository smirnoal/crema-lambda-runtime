package com.smirnoal.crema.log;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StructuredFieldsTest {

    @Test
    void mergeRawJson_parsesAndMerges() {
        StructuredFields fields = StructuredFields.fromRawJson("{\"a\":1,\"b\":true,\"c\":\"x\"}");
        assertEquals(3, fields.asMap().size());
        assertEquals(1, fields.asMap().get("a"));
        assertEquals(Boolean.TRUE, fields.asMap().get("b"));
        assertEquals("x", fields.asMap().get("c"));
    }

    @Test
    void duplicateKeys_lastWriterWins_acrossMergeRawJson() {
        StructuredFields fields = StructuredFields.builder()
                .mergeRawJson("{\"service\":\"A\",\"latency\":42}")
                .mergeRawJson("{\"service\":\"B\",\"region\":\"us-east-1\"}")
                .build();
        assertEquals("B", fields.asMap().get("service"));
        assertEquals(42, fields.asMap().get("latency"));
        assertEquals("us-east-1", fields.asMap().get("region"));
    }

    @Test
    void duplicateKeys_lastWriterWins_acrossPutAndMergeRawJson() {
        StructuredFields fields = StructuredFields.builder()
                .put("service", "A")
                .mergeRawJson("{\"service\":\"B\"}")
                .build();
        assertEquals("B", fields.asMap().get("service"));
    }

    @Test
    void ordering_preserved_insertionOrder() {
        StructuredFields fields = StructuredFields.builder()
                .put("a", "1")
                .put("b", "2")
                .mergeRawJson("{\"c\":3}")
                .put("d", true)
                .build();
        assertEquals(Map.of("a", "1", "b", "2", "c", 3, "d", true).size(), fields.asMap().size());
        assertEquals("[a, b, c, d]", fields.asMap().keySet().toString());
    }

    @Test
    void tryFromRawJson_returnsEmptyOnFailure() {
        StructuredFields fields = StructuredFields.tryFromRawJson("{not-json}");
        assertTrue(fields.isEmpty());
    }

    @Test
    void put_rejectsInvalidKeyCharacters() {
        assertThrows(IllegalArgumentException.class,
                () -> StructuredFields.builder().put("a b", "x"));
        assertThrows(IllegalArgumentException.class,
                () -> StructuredFields.builder().put("a=b", "x"));
        assertThrows(IllegalArgumentException.class,
                () -> StructuredFields.builder().put("a|b", "x"));
        assertThrows(IllegalArgumentException.class,
                () -> StructuredFields.builder().put("a\tb", "x"));
        assertThrows(IllegalArgumentException.class,
                () -> StructuredFields.builder().put("a\nb", "x"));
    }

    @Test
    void mergeRawJson_rejectsInvalidKeys() {
        assertThrows(IllegalArgumentException.class,
                () -> StructuredFields.builder().mergeRawJson("{\"a b\":1}"));
    }

    @Test
    void put_rejectsEmptyAndTooLongKeys() {
        assertThrows(IllegalArgumentException.class,
                () -> StructuredFields.builder().put("", "x"));
        String tooLong = "a".repeat(129);
        assertThrows(IllegalArgumentException.class,
                () -> StructuredFields.builder().put(tooLong, "x"));
    }
}
