package com.smirnoal.crema.log;

/**
 * Holder for the global LambdaLogSink. Set by the runtime during bootstrap.
 * LambdaLogger uses this to route log output.
 */
public final class LambdaLogSinkHolder {

    private static volatile LambdaLogSink sink;

    private LambdaLogSinkHolder() {
    }

    public static LambdaLogSink get() {
        return sink;
    }

    /**
     * Sets the global sink. Called from bootstrap during TelemetryLogRedirection.
     */
    public static void set(LambdaLogSink sink) {
        LambdaLogSinkHolder.sink = sink;
    }
}
