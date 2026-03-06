package com.smirnoal.lambda.serde;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonEscapeTest {

    @Test
    void escape_null() {
        assertEquals("null", JsonEscape.escape(null));
    }

    @Test
    void escape_emptyString() {
        assertEquals("\"\"", JsonEscape.escape(""));
    }

    @Test
    void escape_quotes() {
        assertEquals("\"Hello \\\"world\\\"\"", JsonEscape.escape("Hello \"world\""));
    }

    @Test
    void escape_backslashes() {
        assertEquals("\"path\\\\to\\\\file\"", JsonEscape.escape("path\\to\\file"));
    }

    @Test
    void escape_controlCharacters() {
        assertEquals("\"line1\\nline2\"", JsonEscape.escape("line1\nline2"));
        assertEquals("\"a\\rb\"", JsonEscape.escape("a\rb"));
        assertEquals("\"a\\tb\"", JsonEscape.escape("a\tb"));
        assertEquals("\"a\\bb\"", JsonEscape.escape("a\bb"));
        assertEquals("\"a\\fb\"", JsonEscape.escape("a\fb"));
    }

    @Test
    void escape_unicode() {
        assertEquals("\"emoji: \\ud83d\\ude03\"", JsonEscape.escape("emoji: 😃"));
        assertEquals("\"\\u0416\"", JsonEscape.escape("Ж"));
    }

    @Test
    void escape_plainString() {
        assertEquals("\"hello\"", JsonEscape.escape("hello"));
    }
}
