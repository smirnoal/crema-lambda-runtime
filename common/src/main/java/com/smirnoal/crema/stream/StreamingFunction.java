package com.smirnoal.crema.stream;

/**
 * Functional interface for streaming Lambda handlers.
 * Handlers write to the response stream instead of returning a value.
 *
 * @param <T> the deserialized event type
 */
@FunctionalInterface
public interface StreamingFunction<T> {

    /**
     * Process the event and write the response to the stream.
     *
     * @param event         the deserialized event
     * @param responseStream the stream to write the response to
     * @throws Exception any exception (I/O, business logic) — the runtime reports it via HTTP trailers
     */
    void accept(T event, ResponseStream responseStream) throws Exception;
}
