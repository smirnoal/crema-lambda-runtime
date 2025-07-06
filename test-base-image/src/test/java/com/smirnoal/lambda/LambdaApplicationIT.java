package com.smirnoal.lambda;

import com.google.gson.Gson;
import com.smirnoal.lambda.testcontainers.ColdLambdaContainer;
import com.smirnoal.lambda.rapid.client.dto.ErrorRequest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LambdaApplicationIT {

    @Test
    public void reverseStringTest() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.ReverseStringMain",
            "build/dependencies/reverse-string-handler-1.0-SNAPSHOT.jar"
        );
        String jsonPayload = "Hello Lambda";
        String expected = new StringBuilder(jsonPayload).reverse().toString();
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, jsonPayload);
        assertEquals(expected, result);
    }

    @Test
    public void throwsTest() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.ThrowsHandler",
            "build/dependencies/throws-handler-1.0-SNAPSHOT.jar"
        );
        String jsonPayload = "Hello Lambda";
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, jsonPayload);

        ErrorRequest actualErrorRequest = new Gson().fromJson(result, ErrorRequest.class);
        assertEquals("exception message", actualErrorRequest.errorMessage());
        assertEquals("com.smirnoal.lambda.handlers.ThrowsHandler$MySpecialException", actualErrorRequest.errorType());
        assertTrue(
                actualErrorRequest.stackTrace()[0].startsWith(
                        "com.smirnoal.lambda.handlers.ThrowsHandler.handle(ThrowsHandler.java"));
    }

    @Test
    public void throwsCycleTest() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.ThrowsCycleHandler",
            "build/dependencies/throws-cycle-handler-1.0-SNAPSHOT.jar"
        );
        String jsonPayload = "Hello Lambda";
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, jsonPayload);

        ErrorRequest actualErrorRequest = new Gson().fromJson(result, ErrorRequest.class);
        assertEquals("exception 1", actualErrorRequest.errorMessage());
        assertEquals("java.lang.RuntimeException", actualErrorRequest.errorType());
        assertTrue(actualErrorRequest.stackTrace()[0].startsWith(
                "com.smirnoal.lambda.handlers.ThrowsCycleHandler.handle(ThrowsCycleHandler.java:"
        ));
    }

    @Test
    public void testEnvironment() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.EnvCheckHandler",
            "build/dependencies/env-check-handler-1.0-SNAPSHOT.jar"
        );
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, "");

        assertEquals("", result);
    }

    @Test
    public void testEchoStringHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.EchoStringHandler",
            "build/dependencies/echo-string-handler-1.0-SNAPSHOT.jar"
        );
        String payload = "abcdefg";
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals(payload, result);
    }

    @Test
    public void testConsumerHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.ConsumerHandler",
            "build/dependencies/consumer-handler-1.0-SNAPSHOT.jar"
        );
        String payload = "abcdefg";
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals("", result);
    }

    @Test
    public void testRunnableHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.RunnableHandler",
            "build/dependencies/runnable-handler-1.0-SNAPSHOT.jar"
        );
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, "");

        assertEquals("", result);
    }

    @Test
    public void testReturnNullHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.ReturnNullHandler",
            "build/dependencies/return-null-handler-1.0-SNAPSHOT.jar"
        );
        String payload = "abcdefg";
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals("null", result);
    }

    @Test
    public void testPojoHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.PojoHandler",
            "build/dependencies/pojo-handler-1.0-SNAPSHOT.jar"
        );
        String payload = "{ \"name\": \"John\", \"age\": 23 }";
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals("{\"name\":\"Dublin\",\"yearFounded\":841}", result);
    }

    @Test
    public void testPojoHandler2() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.PojoHandler2",
            "build/dependencies/pojo-handler2-1.0-SNAPSHOT.jar"
        );
        String payload = "{ \"name\": \"John\", \"age\": 23 }";
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals("{\"name\":\"Dublin\",\"yearFounded\":841}", result);
    }

//    @Test
//    public void echoTestLimit() {
//        String handler = "com.smirnoal.lambda.handlers.Echo";
//
//        int maxPayloadSize = 6 * 1024 * 1024 + 101;
//        StringBuilder sb = new StringBuilder(maxPayloadSize);
//        for (int i = 0; i < maxPayloadSize; i++) {
//            sb.append('A');
//        }
//
//        String result = ColdLambdaContainer.invokeLambda(handler, sb.toString());
//        System.out.println(result.length());
//        assertEquals(sb.toString(), result);
//    }
}