package com.smirnoal.crema.stream;

import java.io.OutputStream;

/**
 * User-facing stream for Lambda response streaming.
 * Extends {@link OutputStream} for full Java I/O ecosystem compatibility
 * (PrintWriter, OutputStreamWriter, GZIPOutputStream, etc.).
 * <p>
 * {@link #setContentType(String)} must be called before the first {@link #write(int)} or
 * {@link #write(byte[])}. Calling it after throws {@link IllegalStateException}.
 * Default content-type is {@code application/octet-stream}.
 */
public abstract class ResponseStream extends OutputStream {

    /**
     * Set the Content-Type header for the response.
     * Must be called before the first write; throws IllegalStateException if called too late.
     *
     * @param contentType the content type (e.g., "text/plain; charset=utf-8")
     */
    public abstract void setContentType(String contentType);
}
