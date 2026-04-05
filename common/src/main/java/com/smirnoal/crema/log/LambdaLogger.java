package com.smirnoal.crema.log;

import java.util.function.Supplier;

/**
 * Customer-facing logger for Lambda handlers. Provides level-based logging
 * with support for AWS_LAMBDA_LOG_FORMAT and AWS_LAMBDA_LOG_LEVEL.
 * <p>
 * Obtain via {@link com.smirnoal.crema.Lambda#logger()}.
 */
public final class LambdaLogger {

    private static final LambdaLogger INSTANCE = new LambdaLogger();

    LambdaLogger() {
    }

    public void trace(String message) {
        log(LogLevel.TRACE, message);
    }

    public void trace(String message, StructuredFields fields) {
        log(LogLevel.TRACE, message, null, fields);
    }

    public void trace(Supplier<String> messageSupplier) {
        log(LogLevel.TRACE, messageSupplier);
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public void debug(String message, StructuredFields fields) {
        log(LogLevel.DEBUG, message, null, fields);
    }

    public void debug(Supplier<String> messageSupplier) {
        log(LogLevel.DEBUG, messageSupplier);
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void info(String message, StructuredFields fields) {
        log(LogLevel.INFO, message, null, fields);
    }

    public void info(Supplier<String> messageSupplier) {
        log(LogLevel.INFO, messageSupplier);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public void warn(String message, StructuredFields fields) {
        log(LogLevel.WARN, message, null, fields);
    }

    public void warn(Supplier<String> messageSupplier) {
        log(LogLevel.WARN, messageSupplier);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public void error(String message, StructuredFields fields) {
        if (!isErrorEnabled()) {
            return;
        }
        log(LogLevel.ERROR, message, null, fields);
    }

    public void error(String message, Throwable t) {
        if (!isErrorEnabled()) {
            return;
        }
        log(LogLevel.ERROR, message, t, null);
    }

    public void error(Throwable t) {
        if (!isErrorEnabled()) {
            return;
        }
        String message = t != null && t.getMessage() != null
                ? t.getMessage()
                : (t != null ? t.getClass().getSimpleName() : "UnknownError");
        log(LogLevel.ERROR, message, t, null);
    }

    public void error(Supplier<String> messageSupplier) {
        log(LogLevel.ERROR, messageSupplier);
    }

    public void fatal(String message) {
        log(LogLevel.FATAL, message);
    }

    public void fatal(String message, StructuredFields fields) {
        log(LogLevel.FATAL, message, null, fields);
    }

    public void fatal(Supplier<String> messageSupplier) {
        log(LogLevel.FATAL, messageSupplier);
    }

    public boolean isTraceEnabled() {
        return isEnabled(LogLevel.TRACE);
    }

    public boolean isDebugEnabled() {
        return isEnabled(LogLevel.DEBUG);
    }

    public boolean isInfoEnabled() {
        return isEnabled(LogLevel.INFO);
    }

    public boolean isWarnEnabled() {
        return isEnabled(LogLevel.WARN);
    }

    public boolean isErrorEnabled() {
        return isEnabled(LogLevel.ERROR);
    }

    public boolean isFatalEnabled() {
        return isEnabled(LogLevel.FATAL);
    }

    private void log(LogLevel level, String message) {
        log(level, message, null, null);
    }

    private void log(LogLevel level, String message, Throwable throwable) {
        log(level, message, throwable, null);
    }

    private void log(LogLevel level, String message, Throwable throwable, StructuredFields fields) {
        LambdaLogSink sink = LambdaLogSinkHolder.get();
        if (sink != null) {
            sink.log(level, message, throwable, fields);
        } else {
            System.err.println(message);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }
    }

    private void log(LogLevel level, Supplier<String> messageSupplier) {
        if (!isEnabled(level)) {
            return;
        }
        log(level, messageSupplier.get());
    }

    private boolean isEnabled(LogLevel level) {
        LambdaLogSink sink = LambdaLogSinkHolder.get();
        if (sink == null) {
            return true;
        }
        if (sink instanceof FormattingLogSinkAdapter adapter) {
            return adapter.isEnabled(level);
        }
        return true;
    }

    public static LambdaLogger instance() {
        return INSTANCE;
    }

    /**
     * Adapter to expose isEnabled from FormattingLogSink. FormattingLogSink
     * is in bootstrap; this adapter is used when the sink is available.
     */
    public interface FormattingLogSinkAdapter extends LambdaLogSink {
        boolean isEnabled(LogLevel level);
    }
}
