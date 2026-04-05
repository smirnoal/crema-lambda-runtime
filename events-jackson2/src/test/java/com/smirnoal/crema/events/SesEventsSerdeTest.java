package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SesEventsSerdeTest extends EventsSerdeTestBase {

    @Test
    void roundTripSesEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(SesEvent.class, SesEvent.class);
        String json = loadResource("events/ses-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        SesEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getRecords());
        assertEquals(1, deserialized.getRecords().size());
        assertEquals("aws:ses", deserialized.getRecords().get(0).getEventSource());
        assertEquals("sender@example.com", deserialized.getRecords().get(0).getSes().getMail().getSource());
        assertEquals("recipient@example.com", deserialized.getRecords().get(0).getSes().getReceipt().getRecipients().get(0));

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        SesEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getRecords().get(0).getSes().getMail().getMessageId(),
                roundTripped.getRecords().get(0).getSes().getMail().getMessageId());
    }
}
