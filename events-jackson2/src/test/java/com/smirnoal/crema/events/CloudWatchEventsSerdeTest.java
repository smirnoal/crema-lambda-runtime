package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CloudWatchEventsSerdeTest extends EventsSerdeTestBase {

    @Test
    void roundTripCloudWatchLogsEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(CloudWatchLogsEvent.class, CloudWatchLogsEvent.class);
        String json = loadResource("events/cloudwatchlogs-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        CloudWatchLogsEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getAwslogs());
        assertNotNull(deserialized.getAwslogs().getData());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        CloudWatchLogsEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getAwslogs().getData(), roundTripped.getAwslogs().getData());
    }

    @Test
    void roundTripCloudWatchMetricAlarmEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(CloudWatchMetricAlarmEvent.class, CloudWatchMetricAlarmEvent.class);
        String json = loadResource("events/cloudwatch-metric-alarm-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        CloudWatchMetricAlarmEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("aws.cloudwatch", deserialized.getSource());
        assertEquals("HighCPUUtilization", deserialized.getAlarmData().getAlarmName());
        assertEquals("ALARM", deserialized.getAlarmData().getState().getValue());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        CloudWatchMetricAlarmEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getAlarmData().getAlarmName(), roundTripped.getAlarmData().getAlarmName());
    }

    @Test
    void roundTripCloudWatchCompositeAlarmEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(CloudWatchCompositeAlarmEvent.class, CloudWatchCompositeAlarmEvent.class);
        String json = loadResource("events/cloudwatch-composite-alarm-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        CloudWatchCompositeAlarmEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("aws.cloudwatch", deserialized.getSource());
        assertEquals("CompositeAlarm", deserialized.getAlarmData().getAlarmName());
        assertEquals("ALARM(Alarm1) OR ALARM(Alarm2)", deserialized.getAlarmData().getConfiguration().getAlarmRule());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        CloudWatchCompositeAlarmEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getAlarmData().getAlarmName(), roundTripped.getAlarmData().getAlarmName());
    }
}
