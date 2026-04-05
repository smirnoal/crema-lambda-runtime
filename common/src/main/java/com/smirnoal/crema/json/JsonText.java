package com.smirnoal.crema.json;

import java.util.Objects;

/**
 * Minimal helpers for writing JSON as text without a full serializer: string literals,
 * escaping, and room for related fragments. Appends into a caller-supplied
 * {@link StringBuilder} to avoid extra allocations.
 * <p>
 * Prefer this name over “escape-only” utilities: JSON string values are quoted literals,
 * not raw escaped substrings.
 */
public final class JsonText {

    private JsonText() {
    }

    /**
     * Appends a JSON string literal for {@code input}: opening quote, escaped characters,
     * closing quote. If {@code input} is null, appends the JSON keyword {@code null} (no quotes).
     *
     * @param input Java string to serialize (may be null)
     * @param out   target buffer (must not be null)
     */
    public static void appendJsonString(String input, StringBuilder out) {
        Objects.requireNonNull(out, "out");
        if (input == null) {
            out.append("null");
            return;
        }
        out.append('"');
        appendEscapedBody(input, out);
        out.append('"');
    }

    private static void appendEscapedBody(String input, StringBuilder out) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32 || c > 126) {
                        String hex = Integer.toHexString(c);
                        out.append("\\u");
                        out.append("0".repeat(4 - hex.length()));
                        out.append(hex);
                    } else {
                        out.append(c);
                    }
                }
            }
        }
    }
}
