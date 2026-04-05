package com.smirnoal.crema.log;

import com.smirnoal.crema.Lambda;
import com.smirnoal.crema.log.RicLog.RicLogger;

import java.nio.charset.StandardCharsets;

/**
 * Sink that filters by log level and formats messages (JSON or TEXT) before
 * forwarding to the underlying byte sink. Used by LambdaLogger and
 * FramedTelemetryPrintStream.
 */
public final class FormattingLogSink implements LambdaLogger.FormattingLogSinkAdapter {

    private final LambdaLogFormatter formatter;
    private final LogLevel minimumLogLevel;
    private final boolean filterByLevel;
    private final ByteLogSink delegate;
    private static final RicLogger log = RicLog.getLogger("main");

    public FormattingLogSink(ByteLogSink delegate) {
        this.delegate = delegate;
        this.formatter = LambdaLogFormatter.fromEnvironment();
        this.filterByLevel = formatter.isJsonFormat();
        this.minimumLogLevel = LogLevel.fromString(Lambda.Environment.AWS_LAMBDA_LOG_LEVEL);
    }

    @Override
    public void log(LogLevel level, String message, Throwable throwable, StructuredFields fields) {
        if (message == null) {
            message = "null";
        }
        if (filterByLevel && level != LogLevel.UNDEFINED && !isEnabled(level)) {
            return;
        }
        String formatted;
        try {
            formatted = formatter.format(level, message, throwable, fields);
        } catch (StructuredFieldsSerializationException e) {
            log.exception(
                    "StructuredFields serialization failed; emitting log line without extra fields",
                    e);
            formatted = formatter.format(level, message, throwable, null);
        }
        delegate.log(formatted.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Passes raw bytes through without formatting. Used for binary output
     * from write() that cannot be interpreted as a string message.
     */
    public void logRaw(byte[] bytes) {
        if (bytes != null && bytes.length > 0) {
            delegate.log(bytes);
        }
    }

    @Override
    public boolean isEnabled(LogLevel level) {
        if (!filterByLevel) {
            return true;
        }
        return level == LogLevel.UNDEFINED
                || level.ordinal() >= minimumLogLevel.ordinal();
    }
}
