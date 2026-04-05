package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StreamsEventsSerdeTest extends EventsSerdeTestBase {

    @Test
    void roundTripKinesisEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(KinesisEvent.class, KinesisEvent.class);
        String json = loadResource("events/kinesis-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        KinesisEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getRecords());
        assertEquals(1, deserialized.getRecords().size());
        assertEquals("aws:kinesis", deserialized.getRecords().get(0).getEventSource());
        assertNotNull(deserialized.getRecords().get(0).getKinesis());

        ByteBuffer data = deserialized.getRecords().get(0).getKinesis().getData();
        assertNotNull(data);
        assertEquals("Hello, this is a test 123.", StandardCharsets.UTF_8.decode(data).toString());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        KinesisEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getRecords().get(0).getEventID(), roundTripped.getRecords().get(0).getEventID());
    }

    @Test
    void roundTripDynamodbStreamsEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(DynamodbEvent.class, DynamodbEvent.class);
        String json = loadResource("events/dynamodb-streams-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        DynamodbEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getRecords());
        assertEquals(3, deserialized.getRecords().size());
        assertEquals("INSERT", deserialized.getRecords().get(0).getEventName());
        assertNotNull(deserialized.getRecords().get(0).getDynamodb());

        ByteBuffer payloadB = deserialized.getRecords().get(0).getDynamodb().getNewImage()
                .getAdditionalProperties().get("Payload").getB();
        assertNotNull(payloadB);
        assertEquals("Hello", StandardCharsets.UTF_8.decode(payloadB).toString());

        var binarySet = deserialized.getRecords().get(1).getDynamodb().getNewImage()
                .getAdditionalProperties().get("BinarySet").getBs();
        assertNotNull(binarySet);
        assertEquals(2, binarySet.size());
        assertEquals("a", StandardCharsets.UTF_8.decode(binarySet.get(0)).toString());
        assertEquals("bc", StandardCharsets.UTF_8.decode(binarySet.get(1)).toString());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        DynamodbEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getRecords().get(0).getEventID(), roundTripped.getRecords().get(0).getEventID());
    }

    @Test
    void roundTripDynamodbTimeWindowEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(DynamodbTimeWindowEvent.class, DynamodbTimeWindowEvent.class);
        String json = loadResource("events/dynamodb-time-window-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        DynamodbTimeWindowEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getWindow());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        DynamodbTimeWindowEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getShardId(), roundTripped.getShardId());
    }

    @Test
    void roundTripKinesisFirehoseEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(KinesisFirehoseEvent.class, KinesisFirehoseEvent.class);
        String json = loadResource("events/kinesis-firehose-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        KinesisFirehoseEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("invoked123", deserialized.getInvocationId());
        assertNotNull(deserialized.getRecords());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        KinesisFirehoseEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getDeliveryStreamArn(), roundTripped.getDeliveryStreamArn());
    }

    @Test
    void roundTripKinesisAnalyticsFirehoseInputPreprocessingEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(KinesisAnalyticsFirehoseInputPreprocessingEvent.class, KinesisAnalyticsFirehoseInputPreprocessingEvent.class);
        String json = loadResource("events/kinesis-analytics-firehose-input-preprocessing-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        KinesisAnalyticsFirehoseInputPreprocessingEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getRecords());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        KinesisAnalyticsFirehoseInputPreprocessingEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getInvocationId(), roundTripped.getInvocationId());
    }

    @Test
    void roundTripKinesisAnalyticsStreamsInputPreprocessingEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(KinesisAnalyticsStreamsInputPreprocessingEvent.class, KinesisAnalyticsStreamsInputPreprocessingEvent.class);
        String json = loadResource("events/kinesis-analytics-streams-input-preprocessing-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        KinesisAnalyticsStreamsInputPreprocessingEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getRecords());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        KinesisAnalyticsStreamsInputPreprocessingEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getInvocationId(), roundTripped.getInvocationId());
    }

    @Test
    void roundTripKinesisAnalyticsInputPreprocessingResponse() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(KinesisAnalyticsInputPreprocessingResponse.class, KinesisAnalyticsInputPreprocessingResponse.class);
        String json = loadResource("events/kinesis-analytics-input-preprocessing-response-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        KinesisAnalyticsInputPreprocessingResponse deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getRecords());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        KinesisAnalyticsInputPreprocessingResponse roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertNotNull(roundTripped.getRecords());
    }

    @Test
    void roundTripKinesisAnalyticsOutputDeliveryEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(KinesisAnalyticsOutputDeliveryEvent.class, KinesisAnalyticsOutputDeliveryEvent.class);
        String json = loadResource("events/kinesis-analytics-output-delivery-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        KinesisAnalyticsOutputDeliveryEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getApplicationArn());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        KinesisAnalyticsOutputDeliveryEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getInvocationId(), roundTripped.getInvocationId());
    }

    @Test
    void roundTripKinesisAnalyticsOutputDeliveryResponse() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(KinesisAnalyticsOutputDeliveryResponse.class, KinesisAnalyticsOutputDeliveryResponse.class);
        String json = loadResource("events/kinesis-analytics-output-delivery-response-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        KinesisAnalyticsOutputDeliveryResponse deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getRecords());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        KinesisAnalyticsOutputDeliveryResponse roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertNotNull(roundTripped.getRecords());
    }

    @Test
    void roundTripKinesisTimeWindowEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(KinesisTimeWindowEvent.class, KinesisTimeWindowEvent.class);
        String json = loadResource("events/kinesis-time-window-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        KinesisTimeWindowEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getWindow());
        assertEquals(false, deserialized.getIsFinalInvokeForWindow());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        KinesisTimeWindowEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getShardId(), roundTripped.getShardId());
    }

    @Test
    void roundTripTimeWindowEventResponse() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(TimeWindowEventResponse.class, TimeWindowEventResponse.class);
        String json = loadResource("events/time-window-event-response-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        TimeWindowEventResponse deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getState());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        TimeWindowEventResponse roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertNotNull(roundTripped.getState());
    }
}
