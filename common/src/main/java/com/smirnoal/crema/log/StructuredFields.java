package com.smirnoal.crema.log;

import com.smirnoal.crema.json.MinimalJsonParser;
import com.smirnoal.crema.log.RicLog.RicLogger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable container for structured log fields.
 * <p>
 * Intended to be merged into JSON log output as top-level keys.
 * Ordering is preserved (insertion order). Duplicate keys are resolved using last-writer-wins.
 */
public final class StructuredFields {

    private static final RicLogger log = RicLog.getLogger("main");
    private static final int MAX_KEY_LENGTH = 128;

    static final StructuredFields EMPTY = new StructuredFields(Collections.emptyMap());

    private final Map<String, Object> fields;

    private StructuredFields(Map<String, Object> fields) {
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public static StructuredFields empty() {
        return EMPTY;
    }

    public static StructuredFields of(String key, Object value) {
        return builder().put(key, value).build();
    }

    public static StructuredFields fromRawJson(String json) {
        return builder().mergeRawJson(json).build();
    }

    public static StructuredFields tryFromRawJson(String json) {
        try {
            return fromRawJson(json);
        } catch (IllegalArgumentException e) {
            log.exception("Failed to parse raw JSON for StructuredFields; fields omitted: " + e.getMessage(), e);
            return EMPTY;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    /** Returns an immutable view of fields in insertion order. */
    public Map<String, Object> asMap() {
        return fields;
    }

    public static final class Builder {
        private final LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder put(String key, String value) {
            return putValue(key, value);
        }

        public Builder put(String key, Number value) {
            return putValue(key, value);
        }

        public Builder put(String key, Boolean value) {
            return putValue(key, value);
        }

        public Builder put(String key, Object value) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");
            if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                return putValue(key, value);
            }
            throw new IllegalArgumentException("Unsupported value type: " + value.getClass().getName()
                    + ". Use String, Number, or Boolean. For raw JSON, use mergeRawJson().");
        }

        public Builder mergeRawJson(String json) {
            Objects.requireNonNull(json, "json must not be null");
            Map<String, Object> parsed = MinimalJsonParser.parseObject(json);
            for (Map.Entry<String, Object> e : parsed.entrySet()) {
                String key = e.getKey();
                validateKey(key);
                fields.put(key, e.getValue()); // last-writer-wins
            }
            return this;
        }

        public StructuredFields build() {
            if (fields.isEmpty()) {
                return EMPTY;
            }
            return new StructuredFields(fields);
        }

        private Builder putValue(String key, Object value) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");
            validateKey(key);
            fields.put(key, value); // last-writer-wins
            return this;
        }
    }

    private static void validateKey(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("StructuredFields key must not be empty");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("StructuredFields key too long (max " + MAX_KEY_LENGTH + ")");
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '.' || c == '-';
            if (!ok) {
                throw new IllegalArgumentException(
                        "StructuredFields key contains invalid character '" + c + "': " + key);
            }
        }
    }
}
