package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AwsServiceEventsSerdeTest extends EventsSerdeTestBase {

    @Test
    void roundTripConnectEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(ConnectEvent.class, ConnectEvent.class);
        String json = loadResource("events/connect-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        ConnectEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("ContactFlowEvent", deserialized.getName());
        assertNotNull(deserialized.getDetails());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        ConnectEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getName(), roundTripped.getName());
    }

    @Test
    void roundTripLexEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(LexEvent.class, LexEvent.class);
        String json = loadResource("events/lex-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        LexEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("1.0", deserialized.getMessageVersion());
        assertNotNull(deserialized.getCurrentIntent());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        LexEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getInvocationSource(), roundTripped.getInvocationSource());
    }

    @Test
    void roundTripLexV2Event() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(LexV2Event.class, LexV2Event.class);
        String json = loadResource("events/lexv2-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        LexV2Event deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getBot());
        assertNotNull(deserialized.getSessionState());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        LexV2Event roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getSessionId(), roundTripped.getSessionId());
    }

    @Test
    void roundTripLexV2Response() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(LexV2Response.class, LexV2Response.class);
        String json = loadResource("events/lexv2-response-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        LexV2Response deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getSessionState());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        LexV2Response roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertNotNull(roundTripped.getSessionState());
    }

    @Test
    void roundTripCodeCommitEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(CodeCommitEvent.class, CodeCommitEvent.class);
        String json = loadResource("events/codecommit-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        CodeCommitEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getRecords());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        CodeCommitEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertNotNull(roundTripped.getRecords());
    }

    @Test
    void roundTripCodePipelineEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(CodePipelineEvent.class, CodePipelineEvent.class);
        String json = loadResource("events/codepipeline-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        CodePipelineEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        CodePipelineEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertNotNull(roundTripped);
    }

    @Test
    void roundTripCloudFormationCustomResourceEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(CloudFormationCustomResourceEvent.class, CloudFormationCustomResourceEvent.class);
        String json = loadResource("events/cloudformation-custom-resource-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        CloudFormationCustomResourceEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("Create", deserialized.getRequestType());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        CloudFormationCustomResourceEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getRequestId(), roundTripped.getRequestId());
    }

    @Test
    void roundTripConfigEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(ConfigEvent.class, ConfigEvent.class);
        String json = loadResource("events/config-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        ConfigEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getInvokingEvent());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        ConfigEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getConfigRuleArn(), roundTripped.getConfigRuleArn());
    }

    @Test
    void roundTripSecretsManagerRotationEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(SecretsManagerRotationEvent.class, SecretsManagerRotationEvent.class);
        String json = loadResource("events/secrets-manager-rotation-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        SecretsManagerRotationEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getStep());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        SecretsManagerRotationEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getClientRequestToken(), roundTripped.getClientRequestToken());
    }
}
