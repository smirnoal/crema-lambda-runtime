package com.smirnoal.crema.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTextTest {

    private static String appendedJsonString(String input) {
        StringBuilder sb = new StringBuilder();
        JsonText.appendJsonString(input, sb);
        return sb.toString();
    }

    @Test
    void appendJsonString_null() {
        assertEquals("null", appendedJsonString(null));
    }

    @Test
    void appendJsonString_emptyString() {
        assertEquals("\"\"", appendedJsonString(""));
    }

    @Test
    void appendJsonString_quotes() {
        assertEquals("\"Hello \\\"world\\\"\"", appendedJsonString("Hello \"world\""));
    }

    @Test
    void appendJsonString_backslashes() {
        assertEquals("\"path\\\\to\\\\file\"", appendedJsonString("path\\to\\file"));
    }

    @Test
    void appendJsonString_controlCharacters() {
        assertEquals("\"line1\\nline2\"", appendedJsonString("line1\nline2"));
        assertEquals("\"a\\rb\"", appendedJsonString("a\rb"));
        assertEquals("\"a\\tb\"", appendedJsonString("a\tb"));
        assertEquals("\"a\\bb\"", appendedJsonString("a\bb"));
        assertEquals("\"a\\fb\"", appendedJsonString("a\fb"));
    }

    @Test
    void appendJsonString_unicode() {
        assertEquals("\"emoji: \\ud83d\\ude03\"", appendedJsonString("emoji: 😃"));
        assertEquals("\"\\u0416\"", appendedJsonString("Ж"));
    }

    @Test
    void appendJsonString_plainString() {
        assertEquals("\"hello\"", appendedJsonString("hello"));
    }

    @Test
    void appendJsonString_appendsIntoExistingBuffer() {
        StringBuilder sb = new StringBuilder();
        sb.append("x=");
        JsonText.appendJsonString("a\tb", sb);
        sb.append("&end");
        assertEquals("x=\"a\\tb\"&end", sb.toString());

        StringBuilder solo = new StringBuilder();
        JsonText.appendJsonString("a\tb", solo);
        assertEquals("\"a\\tb\"", solo.toString());
    }

    @Test
    void appendJsonString_null_appendsJsonNullIntoBuffer() {
        StringBuilder sb = new StringBuilder("pre:");
        JsonText.appendJsonString(null, sb);
        assertEquals("pre:null", sb.toString());
    }
}
