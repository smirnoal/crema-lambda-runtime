package com.smirnoal.crema.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Lightweight logging for Crema. Enabled via environment variable
 * {@code CREMA_DEBUG_LOGS} (space-separated category names).
 * Zero cost when disabled.
 */
public final class RicLog {

    private static final class Holder {
        static final Set<String> ENABLED = parse();
    }

    private static final Map<String, RicLogger> LOGGERS = new ConcurrentHashMap<>();

    private RicLog() {
    }

    public static RicLogger getLogger(String name) {
        return LOGGERS.computeIfAbsent(name, RicLogger::new);
    }

    private static Set<String> parse() {
        String v = System.getenv("CREMA_DEBUG_LOGS");
        if (v == null || v.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(v.trim().split("\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Logger for a named category. Obtained via {@link RicLog#getLogger(String)}.
     * Logging is disabled when the category is not listed in {@code CREMA_DEBUG_LOGS}.
     */
    public static final class RicLogger {

        private final String name;

        RicLogger(String name) {
            this.name = name;
        }

        public boolean isEnabled() {
            return Holder.ENABLED.contains(name);
        }

        public void message(String message) {
            if (Holder.ENABLED.contains(name)) {
                System.err.println("[crema][" + name + "] " + message);
            }
        }

        public void message(Supplier<String> messageSupplier) {
            if (Holder.ENABLED.contains(name)) {
                System.err.println("[crema][" + name + "] " + messageSupplier.get());
            }
        }

        /**
         * Logs a message with an associated throwable. Formats the throwable with
         * its message and stack trace. Only formats when the category is enabled.
         */
        public void exception(String message, Throwable throwable) {
            if (Holder.ENABLED.contains(name)) {
                String formatted = formatThrowable(message, throwable);
                System.err.println("[crema][" + name + "] " + formatted);
            }
        }

        /**
         * Logs a throwable with its message and stack trace.
         */
        public void exception(Throwable throwable) {
            if (Holder.ENABLED.contains(name)) {
                String formatted = formatThrowable(null, throwable);
                System.err.println("[crema][" + name + "] " + formatted);
            }
        }

        private static String formatThrowable(String message, Throwable throwable) {
            StringWriter sw = new StringWriter();
            if (message != null && !message.isEmpty()) {
                sw.write(message);
                sw.write("\n");
            }
            if (throwable != null) {
                throwable.printStackTrace(new PrintWriter(sw));
            }
            return sw.toString().trim();
        }
    }
}
