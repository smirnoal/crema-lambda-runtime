package com.smirnoal.crema.stream;

/**
 * Functional interface for streaming Lambda handlers that do not need event input.
 * Use with {@link com.smirnoal.crema.LambdaStreamingHandler#withHandler(StreamOnlyHandler)}.
 */
@FunctionalInterface
public interface StreamOnlyHandler {

    /**
     * Write the response to the stream.
     *
     * @param responseStream the stream to write the response to
     * @throws Exception any exception (I/O, business logic) — the runtime reports it via HTTP trailers
     */
    void accept(ResponseStream responseStream) throws Exception;
}
