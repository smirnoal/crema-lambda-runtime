package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ComputeEventsSerdeTest extends EventsSerdeTestBase {

    @Test
    void roundTripScheduledEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(ScheduledEvent.class, ScheduledEvent.class);
        String json = loadResource("events/scheduled-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        ScheduledEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("0", deserialized.getVersion());
        assertEquals("fae0433b-7a0e-e383-7849-7e10153eaa47", deserialized.getId());
        assertEquals("Scheduled Event", deserialized.getDetailType());
        assertEquals("aws.events", deserialized.getSource());
        assertNotNull(deserialized.getTime());
        assertNotNull(deserialized.getResources());
        assertEquals(1, deserialized.getResources().size());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        ScheduledEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getId(), roundTripped.getId());
    }

    @Test
    void roundTripAlbRequestEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(ApplicationLoadBalancerRequestEvent.class, ApplicationLoadBalancerRequestEvent.class);
        String json = loadResource("events/alb-request-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        ApplicationLoadBalancerRequestEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("GET", deserialized.getHttpMethod());
        assertEquals("/lambda", deserialized.getPath());
        assertNotNull(deserialized.getRequestContext());
        assertNotNull(deserialized.getRequestContext().getElb());
        assertNotNull(deserialized.getRequestContext().getElb().getTargetGroupArn());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        ApplicationLoadBalancerRequestEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getPath(), roundTripped.getPath());
    }

    @Test
    void roundTripAlbResponseEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(ApplicationLoadBalancerResponseEvent.class, ApplicationLoadBalancerResponseEvent.class);
        String json = loadResource("events/alb-response-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        ApplicationLoadBalancerResponseEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals(200L, deserialized.getStatusCode());
        assertEquals("200 OK", deserialized.getStatusDescription());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        ApplicationLoadBalancerResponseEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getStatusDescription(), roundTripped.getStatusDescription());
    }

    @Test
    void roundTripEventBridgeEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(EventBridgeEvent.class, EventBridgeEvent.class);
        String json = loadResource("events/eventbridge-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        EventBridgeEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("0", deserialized.getVersion());
        assertEquals("aws.rds", deserialized.getSource());
        assertNotNull(deserialized.getTime());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        EventBridgeEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getId(), roundTripped.getId());
    }

    @Test
    void roundTripCloudFrontEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(CloudFrontEvent.class, CloudFrontEvent.class);
        String json = loadResource("events/cloudfront-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        CloudFrontEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getRecords());
        assertEquals(1, deserialized.getRecords().size());
        assertEquals("EDFDVBD6EXAMPLE", deserialized.getRecords().get(0).getCf().getConfig().getDistributionId());
        assertEquals("GET", deserialized.getRecords().get(0).getCf().getRequest().getMethod());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        CloudFrontEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getRecords().get(0).getCf().getConfig().getDistributionId(),
                roundTripped.getRecords().get(0).getCf().getConfig().getDistributionId());
    }

    @Test
    void roundTripLambdaDestinationEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(LambdaDestinationEvent.class, LambdaDestinationEvent.class);
        String json = loadResource("events/lambda-destination-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        LambdaDestinationEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("1.0", deserialized.getVersion());
        assertEquals("RetriesExhausted", deserialized.getRequestContext().getCondition());
        assertEquals(2L, deserialized.getRequestContext().getApproximateInvokeCount());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        LambdaDestinationEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getRequestContext().getRequestId(), roundTripped.getRequestContext().getRequestId());
    }
}
