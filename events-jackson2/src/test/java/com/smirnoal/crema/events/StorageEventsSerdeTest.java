package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StorageEventsSerdeTest extends EventsSerdeTestBase {

    @Test
    void roundTripS3Event() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(S3Event.class, S3Event.class);
        String json = loadResource("events/s3-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        S3Event deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getRecords());
        assertEquals(1, deserialized.getRecords().size());
        assertEquals("aws:s3", deserialized.getRecords().get(0).getEventSource());
        assertEquals("example-bucket", deserialized.getRecords().get(0).getS3().getBucket().getName());
        assertEquals("test/key", deserialized.getRecords().get(0).getS3().getObject().getKey());
        assertNotNull(deserialized.getRecords().get(0).getEventTime());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        S3Event roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getRecords().get(0).getS3().getObject().getKey(),
                roundTripped.getRecords().get(0).getS3().getObject().getKey());
    }

    @Test
    void roundTripS3BatchEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(S3BatchEvent.class, S3BatchEvent.class);
        String json = loadResource("events/s3-batch-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        S3BatchEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getTasks());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        S3BatchEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getInvocationId(), roundTripped.getInvocationId());
    }

    @Test
    void roundTripS3BatchResponse() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(S3BatchResponse.class, S3BatchResponse.class);
        String json = loadResource("events/s3-batch-response-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        S3BatchResponse deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getResults());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        S3BatchResponse roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getInvocationId(), roundTripped.getInvocationId());
    }

    @Test
    void roundTripS3BatchEventV2() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(S3BatchEventV2.class, S3BatchEventV2.class);
        String json = loadResource("events/s3-batch-v2-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        S3BatchEventV2 deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("2.0", deserialized.getInvocationSchemaVersion());
        assertNotNull(deserialized.getTasks());
        assertEquals(1, deserialized.getTasks().size());
        assertEquals("source-directory-bucket-name", deserialized.getTasks().get(0).getS3Bucket());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        S3BatchEventV2 roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getInvocationId(), roundTripped.getInvocationId());
    }

    @Test
    void roundTripS3ObjectLambdaEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(S3ObjectLambdaEvent.class, S3ObjectLambdaEvent.class);
        String json = loadResource("events/s3-object-lambda-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        S3ObjectLambdaEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getGetObjectContext());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        S3ObjectLambdaEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getProtocolVersion(), roundTripped.getProtocolVersion());
    }
}
