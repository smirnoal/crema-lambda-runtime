package com.smirnoal.crema.events;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthEventsSerdeTest extends EventsSerdeTestBase {

    @Test
    void roundTripApiGatewayCustomAuthorizerEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(APIGatewayCustomAuthorizerEvent.class, APIGatewayCustomAuthorizerEvent.class);
        String json = loadResource("events/apigw-custom-authorizer-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        APIGatewayCustomAuthorizerEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("1.0", deserialized.getVersion());
        assertEquals("REQUEST", deserialized.getType());
        assertEquals("GET", deserialized.getHttpMethod());
        assertNotNull(deserialized.getHeaders());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        APIGatewayCustomAuthorizerEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getMethodArn(), roundTripped.getMethodArn());
    }

    @Test
    void roundTripApiGatewayV2CustomAuthorizerEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(APIGatewayV2CustomAuthorizerEvent.class, APIGatewayV2CustomAuthorizerEvent.class);
        String json = loadResource("events/apigw-v2-custom-authorizer-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        APIGatewayV2CustomAuthorizerEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("2.0", deserialized.getVersion());
        assertEquals("REQUEST", deserialized.getType());
        assertEquals("$default", deserialized.getRouteKey());
        assertNotNull(deserialized.getRequestContext());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        APIGatewayV2CustomAuthorizerEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getRouteArn(), roundTripped.getRouteArn());
    }

    @Test
    void roundTripCognitoEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(CognitoEvent.class, CognitoEvent.class);
        String json = loadResource("events/cognito-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        CognitoEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("SyncTrigger", deserialized.getEventType());
        assertNotNull(deserialized.getDatasetRecords());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        CognitoEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getIdentityId(), roundTripped.getIdentityId());
    }

    @Test
    void roundTripCognitoUserPoolPreSignUpEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(CognitoUserPoolPreSignUpEvent.class, CognitoUserPoolPreSignUpEvent.class);
        String json = loadResource("events/cognito-user-pool-pre-signup-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        CognitoUserPoolPreSignUpEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals("PreSignUp_SignUp", deserialized.getTriggerSource());
        assertNotNull(deserialized.getRequest());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        CognitoUserPoolPreSignUpEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getUserName(), roundTripped.getUserName());
    }

    @Test
    void roundTripAppSyncLambdaAuthorizerEvent() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(AppSyncLambdaAuthorizerEvent.class, AppSyncLambdaAuthorizerEvent.class);
        String json = loadResource("events/appsync-authorizer-event-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        AppSyncLambdaAuthorizerEvent deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertNotNull(deserialized.getAuthorizationToken());
        assertNotNull(deserialized.getRequestContext());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        AppSyncLambdaAuthorizerEvent roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getAuthorizationToken(), roundTripped.getAuthorizationToken());
    }

    @Test
    void roundTripCognitoUserPoolCreateAuthChallengeEvent() throws IOException {
        roundTripCognitoUserPoolEvent(CognitoUserPoolCreateAuthChallengeEvent.class,
                "events/cognito-user-pool-create-auth-challenge-sample.json", "CreateAuthChallenge_Authentication");
    }

    @Test
    void roundTripCognitoUserPoolCustomMessageEvent() throws IOException {
        roundTripCognitoUserPoolEvent(CognitoUserPoolCustomMessageEvent.class,
                "events/cognito-user-pool-custom-message-sample.json", "CustomMessage_SignUp");
    }

    @Test
    void roundTripCognitoUserPoolCustomSmsSenderEvent() throws IOException {
        roundTripCognitoUserPoolEvent(CognitoUserPoolCustomSmsSenderEvent.class,
                "events/cognito-user-pool-custom-sms-sender-sample.json", "CustomSMSSender_SignUp");
    }

    @Test
    void roundTripCognitoUserPoolDefineAuthChallengeEvent() throws IOException {
        roundTripCognitoUserPoolEvent(CognitoUserPoolDefineAuthChallengeEvent.class,
                "events/cognito-user-pool-define-auth-challenge-sample.json", "DefineAuthChallenge_Authentication");
    }

    @Test
    void roundTripCognitoUserPoolMigrateUserEvent() throws IOException {
        roundTripCognitoUserPoolEvent(CognitoUserPoolMigrateUserEvent.class,
                "events/cognito-user-pool-migrate-user-sample.json", "UserMigration_Authentication");
    }

    @Test
    void roundTripCognitoUserPoolPostAuthenticationEvent() throws IOException {
        roundTripCognitoUserPoolEvent(CognitoUserPoolPostAuthenticationEvent.class,
                "events/cognito-user-pool-post-authentication-sample.json", "PostAuthentication_Authentication");
    }

    @Test
    void roundTripCognitoUserPoolPostConfirmationEvent() throws IOException {
        roundTripCognitoUserPoolEvent(CognitoUserPoolPostConfirmationEvent.class,
                "events/cognito-user-pool-post-confirmation-sample.json", "PostConfirmation_ConfirmSignUp");
    }

    @Test
    void roundTripCognitoUserPoolPreAuthenticationEvent() throws IOException {
        roundTripCognitoUserPoolEvent(CognitoUserPoolPreAuthenticationEvent.class,
                "events/cognito-user-pool-pre-authentication-sample.json", "PreAuthentication_Authentication");
    }

    @Test
    void roundTripCognitoUserPoolPreTokenGenerationEvent() throws IOException {
        roundTripCognitoUserPoolEvent(CognitoUserPoolPreTokenGenerationEvent.class,
                "events/cognito-user-pool-pre-token-generation-sample.json", "TokenGeneration_Authentication");
    }

    @Test
    void roundTripCognitoUserPoolPreTokenGenerationEventV2() throws IOException {
        roundTripCognitoUserPoolEvent(CognitoUserPoolPreTokenGenerationEventV2.class,
                "events/cognito-user-pool-pre-token-generation-v2-sample.json", "TokenGeneration_Authentication");
    }

    @Test
    void roundTripCognitoUserPoolVerifyAuthChallengeResponseEvent() throws IOException {
        roundTripCognitoUserPoolEvent(CognitoUserPoolVerifyAuthChallengeResponseEvent.class,
                "events/cognito-user-pool-verify-auth-challenge-response-sample.json",
                "VerifyAuthChallengeResponse_Authentication");
    }

    @Test
    void roundTripCognitoUserPoolInboundFederationEvent() throws IOException {
        roundTripCognitoUserPoolEvent(CognitoUserPoolInboundFederationEvent.class,
                "events/cognito-user-pool-inbound-federation-sample.json", "InboundFederation_ExternalProvider");
    }

    private <T> void roundTripCognitoUserPoolEvent(Class<T> eventClass, String resourcePath, String expectedTriggerSource)
            throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(eventClass, eventClass);
        String json = loadResource(resourcePath);
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        @SuppressWarnings("unchecked")
        T deserialized = (T) serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals(expectedTriggerSource, getTriggerSource(deserialized));

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        T roundTripped = (T) serde.inputDeserializer().apply(outputBytes);
        assertEquals(getTriggerSource(deserialized), getTriggerSource(roundTripped));
    }

    private String getTriggerSource(Object event) {
        try {
            return (String) event.getClass().getMethod("getTriggerSource").invoke(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void roundTripAppSyncLambdaAuthorizerResponse() throws IOException {
        var serde = EventsJacksonLambdaSerde.forEventAndResponse(AppSyncLambdaAuthorizerResponse.class, AppSyncLambdaAuthorizerResponse.class);
        String json = loadResource("events/appsync-authorizer-response-sample.json");
        byte[] inputBytes = json.getBytes(StandardCharsets.UTF_8);

        AppSyncLambdaAuthorizerResponse deserialized = serde.inputDeserializer().apply(inputBytes);
        assertNotNull(deserialized);
        assertEquals(true, deserialized.getIsAuthorized());

        byte[] outputBytes = serde.outputSerializer().apply(deserialized);
        AppSyncLambdaAuthorizerResponse roundTripped = serde.inputDeserializer().apply(outputBytes);
        assertEquals(deserialized.getIsAuthorized(), roundTripped.getIsAuthorized());
    }
}
