package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiGatewayEventsSerdeTest extends EventsSerdeTestBase {

    @Test
    void roundTripApiGatewayRestEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(ApiGatewayProxyRequestEvent.class, ApiGatewayProxyRequestEvent.class);
        String json = loadResource("events/apigw-rest-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        ApiGatewayProxyRequestEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("GET", deserialized.getHttpMethod());
        assertEquals("/my/path", deserialized.getPath());
        assertEquals("Hello from Lambda!", deserialized.getBody());
        assertNotNull(deserialized.getRequestContext());
        assertEquals("123456789012", deserialized.getRequestContext().getAccountId());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        ApiGatewayProxyRequestEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getPath(), roundTripped.getPath());
    }

    @Test
    void roundTripApiGatewayHttpApiEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(ApiGatewayV2HTTPEvent.class, ApiGatewayV2HTTPEvent.class);
        String json = loadResource("events/apigw-httpapi-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        ApiGatewayV2HTTPEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("$default", deserialized.getRouteKey());
        assertEquals("/my/path", deserialized.getRawPath());
        assertEquals("Hello from Lambda!", deserialized.getBody());
        assertNotNull(deserialized.getRequestContext());
        assertNotNull(deserialized.getRequestContext().getHttp());
        assertEquals("POST", deserialized.getRequestContext().getHttp().getMethod());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        ApiGatewayV2HTTPEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getRouteKey(), roundTripped.getRouteKey());
    }

    @Test
    void roundTripApiGatewayV2WebSocketEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(APIGatewayV2WebSocketEvent.class, APIGatewayV2WebSocketEvent.class);
        String json = loadResource("events/apigw-v2-websocket-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        APIGatewayV2WebSocketEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("POST", deserialized.getHttpMethod());
        assertEquals("/hello/world", deserialized.getPath());
        assertNotNull(deserialized.getRequestContext());
        assertNotNull(deserialized.getRequestContext().getAdditionalProperties().get("eventType"));
        assertNotNull(deserialized.getRequestContext().getAdditionalProperties().get("connectionId"));

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        APIGatewayV2WebSocketEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getRequestContext().getAdditionalProperties().get("connectionId"),
                roundTripped.getRequestContext().getAdditionalProperties().get("connectionId"));
    }

    @Test
    void roundTripApiGatewayV2WebSocketResponse() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(APIGatewayV2WebSocketResponse.class, APIGatewayV2WebSocketResponse.class);
        String json = loadResource("events/apigw-v2-websocket-response-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        APIGatewayV2WebSocketResponse deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals(200L, deserialized.getStatusCode());
        assertEquals("Hello World", deserialized.getBody());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        APIGatewayV2WebSocketResponse roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getBody(), roundTripped.getBody());
    }
}
