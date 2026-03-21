package com.smirnoal.lambda.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Lightweight logging for smirnoal-ric. Enabled via environment variable
 * {@code SMIRNOAL_RIC_LOGS} (space-separated category names).
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
        String v = System.getenv("SMIRNOAL_RIC_LOGS");
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
     * Logging is disabled when the category is not listed in {@code SMIRNOAL_RIC_LOGS}.
     */
    public static final class RicLogger {

        private final String name;

        RicLogger(String name) {
            this.name = name;
        }

        public boolean isEnabled() {
            return Holder.ENABLED.contains(name);
        }

        public void log(String message) {
            if (Holder.ENABLED.contains(name)) {
                System.err.println("[smirnoal-ric][" + name + "] " + message);
            }
        }

        public void log(Supplier<String> messageSupplier) {
            if (Holder.ENABLED.contains(name)) {
                System.err.println("[smirnoal-ric][" + name + "] " + messageSupplier.get());
            }
        }
    }
}
