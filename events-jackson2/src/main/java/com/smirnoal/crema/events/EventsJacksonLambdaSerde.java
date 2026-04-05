package com.smirnoal.crema.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smirnoal.crema.serde.ByteBufferBase64Module;
import com.smirnoal.crema.serde.JacksonLambdaSerde;
import com.smirnoal.crema.serde.LambdaSerde;

/**
 * Lambda serde for AWS event types (SQS, SNS, ScheduledEvent, etc.) using Jackson.
 * Uses an ObjectMapper with {@link ByteBufferBase64Module} for binary fields (SQS messageAttributes,
 * Kinesis data, DynamoDB B/BS) to match managed runtime ByteBuffer API.
 */
public final class EventsJacksonLambdaSerde {

    private static final ObjectMapper EVENTS_OBJECT_MAPPER =
            JacksonLambdaSerde.defaultObjectMapperBuilder()
                    .addModule(new ByteBufferBase64Module())
                    .build();

    private EventsJacksonLambdaSerde() {
    }

    /**
     * Returns a serde for event-only handlers (Consumer-style: event in, no response).
     */
    public static <T> LambdaSerde<T, Void> forEvent(Class<T> eventClass) {
        return JacksonLambdaSerde.forTypes(EVENTS_OBJECT_MAPPER, eventClass, Void.class);
    }

    /**
     * Returns a serde for event handlers that return a response.
     */
    public static <T, R> LambdaSerde<T, R> forEventAndResponse(Class<T> eventClass, Class<R> responseClass) {
        return JacksonLambdaSerde.forTypes(EVENTS_OBJECT_MAPPER, eventClass, responseClass);
    }
}
