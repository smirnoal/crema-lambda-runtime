package com.smirnoal.crema.json;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MinimalJsonParserSecurityTest {

    @Test
    void rejectsDeepNesting() {
        String nested = "{\"a\":".repeat(200) + "1" + "}".repeat(200);
        var ex = assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parseObject(nested));
        assertTrue(ex.getMessage().contains("Nesting too deep"));
    }

    @Test
    void rejectsDeepArrayNesting() {
        String nested = "[".repeat(200) + "1" + "]".repeat(200);
        assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parseObject("{\"a\":" + nested + "}"));
    }

    @Test
    void rejectsOversizedInput() {
        String huge = "{\"k\":\"" + "a".repeat(300_000) + "\"}";
        var ex = assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parseObject(huge));
        assertTrue(ex.getMessage().contains("maximum length"));
    }

    @Test
    void rejectsTooManyObjectKeys() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < 1100; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"k").append(i).append("\":1");
        }
        sb.append("}");
        var ex = assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parseObject(sb.toString()));
        assertTrue(ex.getMessage().contains("maximum"));
    }

    @Test
    void rejectsTooManyArrayElements() {
        StringBuilder sb = new StringBuilder("{\"a\":[");
        for (int i = 0; i < 1100; i++) {
            if (i > 0) sb.append(",");
            sb.append("1");
        }
        sb.append("]}");
        var ex = assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parseObject(sb.toString()));
        assertTrue(ex.getMessage().contains("maximum"));
    }

    @Test
    void rejectsVeryLongNumberLiteral() {
        String longNum = "1".repeat(100);
        var ex = assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parseObject("{\"n\":" + longNum + "}"));
        assertTrue(ex.getMessage().contains("Number literal too long"));
    }

    @Test
    void rejectsTruncatedInput() {
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("{\"key\":"));
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("{\"key"));
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("{"));
    }

    @Test
    void rejectsTrailingGarbage() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parseObject("{\"a\":1} GARBAGE"));
        assertTrue(ex.getMessage().contains("trailing"));
    }

    @Test
    void rejectsNonObjectTopLevel() {
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("[1,2,3]"));
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("\"string\""));
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("null"));
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("42"));
    }

    @Test
    void rejectsInvalidSyntax() {
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("{key: value}"));
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("{'key': 'val'}"));
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("{\"a\":1,}"));
    }

    @Test
    void rejectsInvalidEscapes() {
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("{\"a\":\"\\x41\"}"));
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("{\"a\":\"\\u00\"}"));
    }

    @Test
    void rejectsEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject(""));
        assertThrows(IllegalArgumentException.class, () -> MinimalJsonParser.parseObject("   "));
    }

    @Test
    void handlesLongMaxValue() {
        Map<String, Object> parsed = MinimalJsonParser.parseObject("{\"n\":9223372036854775807}");
        assertEquals(Long.MAX_VALUE, parsed.get("n"));
    }

    @Test
    void handlesNegativeAndFloatNumbers() {
        Map<String, Object> parsed = MinimalJsonParser.parseObject("{\"a\":-0,\"b\":1e10,\"c\":1.5E-3}");
        assertNotNull(parsed.get("a"));
        assertNotNull(parsed.get("b"));
        assertNotNull(parsed.get("c"));
    }

    @Test
    void rejectsNonFiniteNumbers() {
        assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parseObject("{\"n\":1e309}"));
    }

    @Test
    void rejectsLongOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> MinimalJsonParser.parseObject("{\"n\":99999999999999999999}"));
    }

    @Test
    void parsesStringsWithControlCharacters() {
        Map<String, Object> parsed = MinimalJsonParser.parseObject("{\"msg\":\"line1\\nline2\\r\\nline3\"}");
        assertEquals("line1\nline2\r\nline3", parsed.get("msg"));
    }

    @Test
    void parsesUnicodeEscapes() {
        Map<String, Object> parsed = MinimalJsonParser.parseObject("{\"c\":\"\\u2603\"}");
        assertEquals("\u2603", parsed.get("c"));
    }
}

