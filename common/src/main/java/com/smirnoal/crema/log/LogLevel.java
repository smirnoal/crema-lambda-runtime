package com.smirnoal.crema.log;

/**
 * Log levels for the Lambda logger. Matches AWS Lambda LogLevel semantics.
 * UNDEFINED is used when the level is not known (e.g. raw System.out); such
 * messages are never filtered.
 */
public enum LogLevel {
    UNDEFINED,     // Level unknown; messages are never filtered
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL;

    public static LogLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNDEFINED;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNDEFINED;
        }
    }
}
