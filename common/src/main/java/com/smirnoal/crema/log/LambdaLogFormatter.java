package com.smirnoal.crema.log;

import com.smirnoal.crema.Lambda;
import com.smirnoal.crema.Lambda.Constants;
import com.smirnoal.crema.json.JsonText;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Formats log messages for TEXT or JSON output based on AWS_LAMBDA_LOG_FORMAT.
 * When format is JSON, injects timestamp, level, message, and requestId from
 * Lambda.context().
 */
public final class LambdaLogFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .withZone(ZoneId.of("UTC"));

    /** Tab delimiter for TEXT format (matches Node.js/Python managed runtimes). */
    private static final char FIELD_DELIMITER = '\t';

    private final boolean jsonFormat;

    private static final RicLog.RicLogger log = RicLog.getLogger("main");

    private static final Set<String> RESERVED_JSON_KEYS = Set.of(
            "timestamp", "level", "message", "requestId",
            "errorType", "errorMessage", "stackTrace"
    );

    LambdaLogFormatter(boolean jsonFormat) {
        this.jsonFormat = jsonFormat;
    }

    public static LambdaLogFormatter fromEnvironment() {
        String format = Lambda.Environment.AWS_LAMBDA_LOG_FORMAT;
        boolean json = format != null
                && Constants.AWS_LAMBDA_LOG_FORMAT_JSON.equalsIgnoreCase(format.trim());
        return new LambdaLogFormatter(json);
    }

    /** Returns true when format is JSON (level filtering applies). */
    public boolean isJsonFormat() {
        return jsonFormat;
    }

    /**
     * Formats a message for output. For JSON format, produces a single-line JSON
     * object. For TEXT, uses timestamp, requestId, level, message (tab-separated)
     * to match Node.js and Python managed runtime plain text format.
     */
    public String format(LogLevel level, String message) {
        return format(level, message, null, null);
    }

    /**
     * Formats a message for output, optionally including error fields when a
     * throwable is provided. When throwable is non-null and format is JSON,
     * adds errorType, errorMessage, and stackTrace
     */
    public String format(LogLevel level, String message, Throwable throwable) {
        return format(level, message, throwable, null);
    }

    public String format(LogLevel level, String message, Throwable throwable, StructuredFields fields) {
        if (message == null) {
            message = "null";
        }
        if (jsonFormat) {
            return formatJson(level, message, throwable, fields);
        }
        return formatText(level, message, throwable, fields);
    }

    private String formatJson(LogLevel level, String message, Throwable throwable, StructuredFields fields) {
        String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());
        var ctx = Lambda.context();
        String requestId = ctx != null ? Objects.requireNonNullElse(ctx.getAwsRequestId(), "") : "";

        StringBuilder sb = new StringBuilder();
        sb.append("{\"timestamp\":");
        JsonText.appendJsonString(timestamp, sb);
        sb.append(",\"level\":");
        JsonText.appendJsonString(level.name(), sb);
        sb.append(",\"message\":");
        JsonText.appendJsonString(message, sb);
        sb.append(",\"requestId\":");
        JsonText.appendJsonString(requestId, sb);
        if (fields != null && !fields.isEmpty()) {
            try {
                appendStructuredFieldsJson(sb, fields);
            } catch (Exception e) {
                throw new StructuredFieldsSerializationException(e);
            }
        }
        if (throwable != null) {
            String errorType = throwable.getClass().getSimpleName();
            sb.append(",\"errorType\":");
            JsonText.appendJsonString(errorType, sb);
            sb.append(",\"errorMessage\":");
            JsonText.appendJsonString(Objects.requireNonNullElse(throwable.getMessage(), ""), sb);
            sb.append(",\"stackTrace\":[");
            List<String> lines = collectStackTraceLines(throwable);
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                JsonText.appendJsonString(lines.get(i), sb);
            }
            sb.append("]");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String formatText(LogLevel level, String message, Throwable throwable, StructuredFields fields) {
        String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());
        var ctx = Lambda.context();
        String requestId = ctx != null ? Objects.requireNonNullElse(ctx.getAwsRequestId(), "") : "";
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp).append(FIELD_DELIMITER);
        sb.append(requestId).append(FIELD_DELIMITER);
        sb.append(level.name()).append(FIELD_DELIMITER);
        sb.append(message);
        if (fields != null && !fields.isEmpty()) {
            sb.append(" |");
            try {
                appendStructuredFieldsText(sb, fields);
            } catch (Exception e) {
                throw new StructuredFieldsSerializationException(e);
            }
        }
        if (throwable != null) {
            sb.append("\n");
            appendStackTrace(sb, throwable);
        }
        sb.append("\n");
        return sb.toString();
    }

    private void appendStructuredFieldsJson(StringBuilder sb, StructuredFields fields) {
        for (Map.Entry<String, Object> e : fields.asMap().entrySet()) {
            String key = e.getKey();
            if (RESERVED_JSON_KEYS.contains(key)) {
                log.message("Dropping reserved StructuredFields key: " + key);
                continue;
            }
            sb.append(',');
            JsonText.appendJsonString(key, sb);
            sb.append(':');
            appendJsonValue(sb, e.getValue());
        }
    }

    private static void appendStructuredFieldsText(StringBuilder sb, StructuredFields fields) {
        for (Map.Entry<String, Object> e : fields.asMap().entrySet()) {
            sb.append(" ").append(e.getKey()).append("=");
            appendTextValue(sb, e.getValue());
        }
    }

    private static void appendJsonValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof String s) {
            JsonText.appendJsonString(s, sb);
            return;
        }
        if (value instanceof Number n) {
            if (n instanceof Double d && !Double.isFinite(d)) {
                sb.append("null");
                return;
            }
            if (n instanceof Float f && !Float.isFinite(f)) {
                sb.append("null");
                return;
            }
            sb.append(n);
            return;
        }
        if (value instanceof Boolean b) {
            sb.append(b);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            sb.append("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!(e.getKey() instanceof String k)) {
                    continue;
                }
                if (!first) sb.append(",");
                first = false;
                JsonText.appendJsonString(k, sb);
                sb.append(':');
                appendJsonValue(sb, e.getValue());
            }
            sb.append("}");
            return;
        }
        if (value instanceof List<?> list) {
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                appendJsonValue(sb, list.get(i));
            }
            sb.append("]");
            return;
        }
        JsonText.appendJsonString(String.valueOf(value), sb);
    }

    private static void appendTextValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof String s) {
            JsonText.appendJsonString(s, sb);
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
            return;
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            StringBuilder tmp = new StringBuilder();
            appendJsonValue(tmp, value);
            sb.append(tmp);
            return;
        }
        sb.append(value);
    }

    private static void appendStackTrace(StringBuilder sb, Throwable throwable) {
        appendStackTrace(sb, throwable, null);
    }

    private static void appendStackTrace(StringBuilder sb, Throwable throwable, List<Throwable> seen) {
        List<Throwable> chain = seen != null ? seen : new ArrayList<>();
        if (chain.contains(throwable)) {
            sb.append("\t... (cause cycle)\n");
            return;
        }
        chain.add(throwable);

        if (chain.size() == 1) {
            sb.append(throwable).append("\n");
        }
        for (StackTraceElement el : throwable.getStackTrace()) {
            sb.append("\tat ").append(el.toString()).append("\n");
        }

        Throwable cause = throwable.getCause();
        if (cause != null) {
            sb.append("Caused by: ").append(cause).append("\n");
            appendStackTrace(sb, cause, chain);
        }

        for (Throwable suppressed : throwable.getSuppressed()) {
            sb.append("Suppressed: ").append(suppressed).append("\n");
            appendStackTrace(sb, suppressed, chain);
        }
    }

    /**
     * Collects stack trace lines in printStackTrace order: frames, then
     * "Caused by: X: msg", cause's frames, then "Suppressed: Y: msg", etc.
     * Used for JSON flat stackTrace array.
     */
    private static List<String> collectStackTraceLines(Throwable throwable) {
        List<String> lines = new ArrayList<>();
        collectStackTraceLines(throwable, lines, new ArrayList<>(), true);
        return lines;
    }

    /**
     * @param emitHeaderForThrowable when true, prepend {@link Throwable#toString()} before stack frames
     *        (matches the first line of {@code printStackTrace} for the root throwable only).
     */
    private static void collectStackTraceLines(
            Throwable throwable, List<String> lines, List<Throwable> seen, boolean emitHeaderForThrowable) {
        if (seen.contains(throwable)) {
            lines.add("... (cause cycle)");
            return;
        }
        seen.add(throwable);

        if (emitHeaderForThrowable) {
            lines.add(throwable.toString());
        }

        for (StackTraceElement el : throwable.getStackTrace()) {
            lines.add(el.toString());
        }

        Throwable cause = throwable.getCause();
        if (cause != null) {
            lines.add("Caused by: " + cause);
            collectStackTraceLines(cause, lines, seen, false);
        }

        for (Throwable suppressed : throwable.getSuppressed()) {
            lines.add("Suppressed: " + suppressed);
            collectStackTraceLines(suppressed, lines, seen, false);
        }
    }
}
