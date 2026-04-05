package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IoTEventsSerdeTest extends EventsSerdeTestBase {

    @Test
    void roundTripIoTButtonEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(IoTButtonEvent.class, IoTButtonEvent.class);
        String json = loadResource("events/iot-button-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        IoTButtonEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("SINGLE", deserialized.getClickType());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        IoTButtonEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getSerialNumber(), roundTripped.getSerialNumber());
    }

    @Test
    void roundTripIoTCustomAuthorizerEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(IoTCustomAuthorizerEvent.class, IoTCustomAuthorizerEvent.class);
        String json = loadResource("events/iot-custom-authorizer-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        IoTCustomAuthorizerEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals(true, deserialized.getSignatureVerified());
        assertNotNull(deserialized.getProtocols());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        IoTCustomAuthorizerEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getToken(), roundTripped.getToken());
    }
}
