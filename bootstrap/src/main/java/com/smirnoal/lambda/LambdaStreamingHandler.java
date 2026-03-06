package com.smirnoal.lambda;

import com.smirnoal.lambda.stream.ResponseStream;
import com.smirnoal.lambda.stream.StreamingFunction;

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

    public LambdaStreamingHandler<T> withInputTypeDeserializer(Function<byte[], T> func) {
        this.inputDeserializer = func;
        return this;
    }

    public LambdaStreamingHandler<T> withHandler(StreamingFunction<T> handler) {
        this.handler = handler;
        return this;
    }

    T toInputType(byte[] bytes) {
        if (inputDeserializer == null) {
            throw new IllegalStateException("Input deserializer must be set");
        }
        return inputDeserializer.apply(bytes);
    }

    void handle(T event, ResponseStream responseStream) throws Exception {
        if (handler == null) {
            throw new IllegalStateException("Handler must be set");
        }
        handler.accept(event, responseStream);
    }
}
