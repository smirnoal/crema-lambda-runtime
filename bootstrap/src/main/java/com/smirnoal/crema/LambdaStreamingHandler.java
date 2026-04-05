package com.smirnoal.crema;

import com.smirnoal.crema.serde.LambdaSerde;
import com.smirnoal.crema.stream.ResponseStream;
import com.smirnoal.crema.stream.StreamOnlyHandler;
import com.smirnoal.crema.stream.StreamingFunction;

import java.util.function.Function;

/**
 * Handler for streaming Lambda responses.
 * Unlike {@link LambdaHandler}, there is no return value — the handler writes directly to the response stream.
 *
 * @param <T> the deserialized event type
 */
public class LambdaStreamingHandler<T> {

    private Function<byte[], T> inputDeserializer;
    private StreamingFunction<T> handler;
    private StreamOnlyHandler streamOnlyHandler;

    public LambdaStreamingHandler<T> withInputTypeDeserializer(Function<byte[], T> func) {
        this.inputDeserializer = func;
        return this;
    }

    /**
     * Configure the input deserializer using an existing {@link LambdaSerde}.
     * The output serializer is ignored since streaming handlers write directly to the response stream.
     */
    public LambdaStreamingHandler<T> withLambdaSerde(LambdaSerde<T, ?> serde) {
        if (serde == null) {
            throw new IllegalArgumentException("serde must not be null");
        }
        return withInputTypeDeserializer(serde.inputDeserializer());
    }

    public LambdaStreamingHandler<T> withHandler(StreamingFunction<T> handler) {
        if (streamOnlyHandler != null) {
            throw new IllegalStateException("Stream-only handler is already set");
        }
        this.handler = handler;
        return this;
    }

    public LambdaStreamingHandler<T> withHandler(StreamOnlyHandler streamOnlyHandler) {
        if (handler != null) {
            throw new IllegalStateException("Event handler is already set");
        }
        this.streamOnlyHandler = streamOnlyHandler;
        return this;
    }

    T toInputType(byte[] bytes) {
        if (streamOnlyHandler != null) {
            return null;
        }
        if (inputDeserializer == null) {
            throw new IllegalStateException("Input deserializer must be set");
        }
        return inputDeserializer.apply(bytes);
    }

    void handle(T event, ResponseStream responseStream) throws Exception {
        if (streamOnlyHandler != null) {
            streamOnlyHandler.accept(responseStream);
        } else if (handler != null) {
            handler.accept(event, responseStream);
        } else {
            throw new IllegalStateException("Handler must be set");
        }
    }
}
