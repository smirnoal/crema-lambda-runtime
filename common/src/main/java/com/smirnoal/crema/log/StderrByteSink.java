package com.smirnoal.crema.log;

/**
 * Byte sink that writes to stderr. Used when telemetry FD is not available.
 */
public final class StderrByteSink implements ByteLogSink {

    @Override
    public void log(byte[] message) {
        if (message != null && message.length > 0) {
            try {
                System.err.write(message);
                System.err.flush();
            } catch (Exception ignored) {
            }
        }
    }
}
