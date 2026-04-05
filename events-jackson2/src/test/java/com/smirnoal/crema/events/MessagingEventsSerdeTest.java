package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MessagingEventsSerdeTest extends EventsSerdeTestBase {

    @Test
    void roundTripSqsEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(SqsEvent.class, SqsEvent.class);
        String json = loadResource("events/sqs-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        SqsEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getRecords());
        assertEquals(1, deserialized.getRecords().size());
        assertEquals("d9144555-9a4f-4ec3-99a0-fc4e625a8db2", deserialized.getRecords().get(0).getMessageId());
        assertEquals("Test message", deserialized.getRecords().get(0).getBody());
        assertEquals("arn:aws:sqs:eu-central-1:123456789012:TestLambda",
                deserialized.getRecords().get(0).getEventSourceARN());

        ByteBuffer binaryValue = deserialized.getRecords().get(0).getMessageAttributes()
                .getAdditionalProperties().get("binaryAttr").getBinaryValue();
        assertNotNull(binaryValue);
        assertEquals("Hello", StandardCharsets.UTF_8.decode(binaryValue).toString());

        var binaryList = deserialized.getRecords().get(0).getMessageAttributes()
                .getAdditionalProperties().get("binaryListAttr").getBinaryListValues();
        assertNotNull(binaryList);
        assertEquals(2, binaryList.size());
        assertEquals("a", StandardCharsets.UTF_8.decode(binaryList.get(0)).toString());
        assertEquals("bc", StandardCharsets.UTF_8.decode(binaryList.get(1)).toString());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        SqsEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getRecords().get(0).getMessageId(), roundTripped.getRecords().get(0).getMessageId());
    }

    @Test
    void roundTripSnsEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(SnsEvent.class, SnsEvent.class);
        String json = loadResource("events/sns-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        SnsEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getRecords());
        assertEquals(1, deserialized.getRecords().size());
        assertEquals("aws:sns", deserialized.getRecords().get(0).getEventSource());
        assertNotNull(deserialized.getRecords().get(0).getSns());
        assertEquals("dc918f50-80c6-56a2-ba33-d8a9bbf013ab", deserialized.getRecords().get(0).getSns().getMessageId());
        assertEquals("Test sns message", deserialized.getRecords().get(0).getSns().getSubject());
        assertNotNull(deserialized.getRecords().get(0).getSns().getTimestamp());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        SnsEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getRecords().get(0).getSns().getMessageId(),
                roundTripped.getRecords().get(0).getSns().getMessageId());
    }
}
