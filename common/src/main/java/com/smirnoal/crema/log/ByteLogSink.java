package com.smirnoal.crema.log;

/**
 * Sink for raw byte output. Used by FormattingLogSink to forward
 * formatted or raw bytes to the underlying transport.
 */
public interface ByteLogSink {

    void log(byte[] message);
}
