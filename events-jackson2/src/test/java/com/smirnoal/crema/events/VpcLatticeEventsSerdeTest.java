package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VpcLatticeEventsSerdeTest extends EventsSerdeTestBase {

    @Test
    void roundTripVpcLatticeV2RequestEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(VpcLatticeV2RequestEvent.class, VpcLatticeV2RequestEvent.class);
        String json = loadResource("events/vpc-lattice-v2-request-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        VpcLatticeV2RequestEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("2.0", deserialized.getVersion());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        VpcLatticeV2RequestEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getMethod(), roundTripped.getMethod());
    }
}
