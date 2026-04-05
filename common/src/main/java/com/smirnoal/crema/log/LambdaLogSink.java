package com.smirnoal.crema.log;

/**
 * Sink for log messages. Implementations format (TEXT or JSON) and forward
 * to the underlying output (telemetry FD, stderr, etc.).
 */
public interface LambdaLogSink {

    void log(LogLevel level, String message, Throwable throwable, StructuredFields fields);
}
