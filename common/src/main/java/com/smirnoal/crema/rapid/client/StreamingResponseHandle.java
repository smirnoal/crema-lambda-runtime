package com.smirnoal.crema.rapid.client;

import com.smirnoal.crema.stream.ResponseStream;

import java.io.IOException;

/**
 * Runtime-internal handle for a streaming response.
 * The runtime calls {@link #complete()} on success or {@link #fail(LambdaError)} on error.
 * Users never see this — they only interact with {@link #responseStream()}.
 */
public interface StreamingResponseHandle {

    /**
     * The stream the handler writes to.
     */
    ResponseStream responseStream();

    /**
     * Signal successful completion of the streaming response.
     * Sends the final chunk with no trailers.
     */
    void complete() throws IOException;

    /**
     * Signal a mid-stream failure.
     * Sends the error as HTTP trailers so the Lambda service can report the failure.
     */
    void fail(LambdaError error) throws IOException;
}
