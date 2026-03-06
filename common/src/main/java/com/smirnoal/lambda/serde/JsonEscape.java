package com.smirnoal.lambda.serde;

/**
 * JSON string escaping utility.
 * Used for building valid JSON output without a full JSON library.
 */
public final class JsonEscape {

    private JsonEscape() {
    }

    /**
     * Escape a string for JSON output.
     * Handles quotes, backslashes, control characters, and Unicode escapes.
     *
     * @param input the string to escape
     * @return JSON-escaped string including surrounding quotes, or "null" if input is null
     */
    public static String escape(String input) {
        if (input == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder().append('"');
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32 || c > 126) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
