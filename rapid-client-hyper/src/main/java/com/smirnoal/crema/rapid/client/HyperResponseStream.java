package com.smirnoal.crema.rapid.client;

import com.smirnoal.crema.stream.ResponseStream;

import java.io.IOException;

/**
 * Native-backed {@link ResponseStream} for Rust + hyper streaming POST.
 *
 * <p>The underlying HTTP POST is deferred until the first {@link #write} (or until the session is
 * completed/failed), so {@link #setContentType} can be called before writing and the value will be
 * sent on the wire.
 */
final class HyperResponseStream extends ResponseStream {

    private static final String STATUS_READY = "ready";
    private static final String STATUS_WRITTEN = "written";

    private final long nativeHandle;
    private final long sessionHandle;
    private volatile String status = STATUS_READY;

    HyperResponseStream(long nativeHandle, long sessionHandle) {
        this.nativeHandle = nativeHandle;
        this.sessionHandle = sessionHandle;
    }

    @Override
    public void setContentType(String contentType) {
        if (!STATUS_READY.equals(status)) {
            throw new IllegalStateException("Cannot set content-type after first write");
        }
        HyperLambdaRapidHttpClient.nativeStreamingSetContentType(
                nativeHandle, sessionHandle, contentType);
    }

    /**
     * Writes a single byte. Each call allocates a one-element array and makes a JNI round-trip;
     * callers that write byte-by-byte (e.g. via a wrapping {@code PrintStream}) should wrap this
     * stream in a {@code BufferedOutputStream} to batch writes.
     */
    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len <= 0) {
            return;
        }
        status = STATUS_WRITTEN;
        HyperLambdaRapidHttpClient.nativeStreamingWrite(nativeHandle, sessionHandle, b, off, len);
    }

    @Override
    public void flush() {
        // Writes flush through native layer immediately.
    }

    @Override
    public void close() {
        // Lifecycle is managed by HyperStreamingResponseHandle.complete() / fail().
    }
}
