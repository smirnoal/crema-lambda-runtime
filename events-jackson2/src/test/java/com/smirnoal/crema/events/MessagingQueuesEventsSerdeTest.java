package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MessagingQueuesEventsSerdeTest extends EventsSerdeTestBase {

    @Test
    void roundTripKafkaEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(KafkaEvent.class, KafkaEvent.class);
        String json = loadResource("events/kafka-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        KafkaEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("aws:kafka", deserialized.getEventSource());
        assertNotNull(deserialized.getRecords());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        KafkaEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getEventSourceArn(), roundTripped.getEventSourceArn());
    }

    @Test
    void roundTripActiveMqEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(ActiveMQEvent.class, ActiveMQEvent.class);
        String json = loadResource("events/activemq-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        ActiveMQEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("aws:mq", deserialized.getEventSource());
        assertNotNull(deserialized.getMessages());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        ActiveMQEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getEventSourceArn(), roundTripped.getEventSourceArn());
    }

    @Test
    void roundTripRabbitMqEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(RabbitMQEvent.class, RabbitMQEvent.class);
        String json = loadResource("events/rabbitmq-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        RabbitMQEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("aws:rmq", deserialized.getEventSource());
        assertNotNull(deserialized.getRmqMessagesByQueue());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        RabbitMQEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getEventSourceArn(), roundTripped.getEventSourceArn());
    }
}
