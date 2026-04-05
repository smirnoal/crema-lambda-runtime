package com.smirnoal.crema.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal, zero-dependency JSON parser intended for parsing untrusted JSON safely.
 * <p>
 * Parses JSON objects into standard Java types:
 * - Object: {@code Map<String, Object>} (LinkedHashMap)
 * - Array: {@code List<Object>}
 * - String: {@code String}
 * - Number: {@code Integer}/{@code Long} for integers, {@code Double} for floats/exponents
 * - Boolean: {@code Boolean}
 * - null: {@code null}
 * <p>
 * The parser enforces hard limits (depth, input length, container sizes, number literal length)
 * and throws {@link IllegalArgumentException} for any malformed input.
 */
public final class MinimalJsonParser {

    public static final int MAX_DEPTH = 128;
    public static final int MAX_INPUT_LENGTH = 256 * 1024;
    public static final int MAX_ENTRIES = 1024;
    public static final int MAX_NUMBER_LENGTH = 30;

    private final String input;
    private int pos;
    private int depth;

    private MinimalJsonParser(String input) {
        this.input = input;
        this.pos = 0;
        this.depth = 0;
    }

    public static Map<String, Object> parseObject(String json) {
        if (json == null) {
            throw new IllegalArgumentException("JSON input must not be null");
        }
        if (json.length() > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException("JSON input exceeds maximum length of " + MAX_INPUT_LENGTH);
        }
        String trimmed = json.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("JSON input must not be empty");
        }

        MinimalJsonParser parser = new MinimalJsonParser(trimmed);
        Object result = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos != parser.input.length()) {
            throw parser.error("Unexpected trailing content");
        }
        if (!(result instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Expected JSON object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        return map;
    }

    private Object readValue() {
        skipWhitespace();
        if (pos >= input.length()) {
            throw error("Unexpected end of input");
        }
        char c = input.charAt(pos);
        return switch (c) {
            case '"' -> readString();
            case '{' -> readObject();
            case '[' -> readArray();
            case 't', 'f' -> readBoolean();
            case 'n' -> readNull();
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) {
                    yield readNumber();
                }
                throw error("Unexpected character: '" + c + "'");
            }
        };
    }

    private Map<String, Object> readObject() {
        if (++depth > MAX_DEPTH) {
            throw error("Nesting too deep (max " + MAX_DEPTH + ")");
        }
        expect('{');
        Map<String, Object> map = new LinkedHashMap<>();
        skipWhitespace();
        if (pos < input.length() && input.charAt(pos) == '}') {
            pos++;
            depth--;
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            expect(':');
            Object value = readValue();
            map.put(key, value);
            if (map.size() > MAX_ENTRIES) {
                throw error("Object exceeds maximum of " + MAX_ENTRIES + " entries");
            }
            skipWhitespace();
            if (pos >= input.length()) {
                throw error("Unexpected end of input in object");
            }
            if (input.charAt(pos) == '}') {
                pos++;
                depth--;
                return map;
            }
            expect(',');
        }
    }

    private List<Object> readArray() {
        if (++depth > MAX_DEPTH) {
            throw error("Nesting too deep (max " + MAX_DEPTH + ")");
        }
        expect('[');
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        if (pos < input.length() && input.charAt(pos) == ']') {
            pos++;
            depth--;
            return list;
        }
        while (true) {
            list.add(readValue());
            if (list.size() > MAX_ENTRIES) {
                throw error("Array exceeds maximum of " + MAX_ENTRIES + " entries");
            }
            skipWhitespace();
            if (pos >= input.length()) {
                throw error("Unexpected end of input in array");
            }
            if (input.charAt(pos) == ']') {
                pos++;
                depth--;
                return list;
            }
            expect(',');
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                if (pos >= input.length()) {
                    throw error("Unexpected end of input in string escape");
                }
                char escaped = input.charAt(pos++);
                switch (escaped) {
                    case '"', '\\', '/' -> sb.append(escaped);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > input.length()) {
                            throw error("Incomplete unicode escape");
                        }
                        String hex = input.substring(pos, pos + 4);
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            throw error("Invalid unicode escape");
                        }
                        pos += 4;
                    }
                    default -> throw error("Invalid escape: '\\" + escaped + "'");
                }
            } else {
                sb.append(c);
            }
        }
        throw error("Unterminated string");
    }

    private Number readNumber() {
        int start = pos;
        if (pos < input.length() && input.charAt(pos) == '-') {
            pos++;
        }
        if (pos >= input.length()) {
            throw error("Unexpected end of input in number");
        }

        if (input.charAt(pos) == '0') {
            pos++;
        } else if (input.charAt(pos) >= '1' && input.charAt(pos) <= '9') {
            while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {
                pos++;
            }
        } else {
            throw error("Invalid number");
        }

        boolean isFloat = false;
        if (pos < input.length() && input.charAt(pos) == '.') {
            isFloat = true;
            pos++;
            if (pos >= input.length() || input.charAt(pos) < '0' || input.charAt(pos) > '9') {
                throw error("Invalid number: no digits after decimal point");
            }
            while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {
                pos++;
            }
        }
        if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
            isFloat = true;
            pos++;
            if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                pos++;
            }
            if (pos >= input.length() || input.charAt(pos) < '0' || input.charAt(pos) > '9') {
                throw error("Invalid number: no digits in exponent");
            }
            while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {
                pos++;
            }
        }

        String numStr = input.substring(start, pos);
        if (numStr.length() > MAX_NUMBER_LENGTH) {
            throw error("Number literal too long");
        }

        try {
            if (isFloat) {
                double v = Double.parseDouble(numStr);
                if (!Double.isFinite(v)) {
                    throw error("Non-finite number");
                }
                return v;
            }
            long val = Long.parseLong(numStr);
            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                return (int) val;
            }
            return val;
        } catch (NumberFormatException e) {
            throw error("Invalid number");
        }
    }

    private Boolean readBoolean() {
        if (input.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (input.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw error("Expected 'true' or 'false'");
    }

    private Object readNull() {
        if (input.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw error("Expected 'null'");
    }

    private void skipWhitespace() {
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return;
            }
            pos++;
        }
    }

    private void expect(char expected) {
        skipWhitespace();
        if (pos >= input.length() || input.charAt(pos) != expected) {
            throw error("Expected '" + expected + "'");
        }
        pos++;
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException(msg + " at position " + pos);
    }
}
