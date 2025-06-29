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
            "build/lib/reverse-string-handler-1.0-SNAPSHOT.jar"
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
            "build/lib/throws-handler-1.0-SNAPSHOT.jar"
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
            "build/lib/throws-cycle-handler-1.0-SNAPSHOT.jar"
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
            "build/lib/env-check-handler-1.0-SNAPSHOT.jar"
        );
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, "");

        assertEquals("", result);
    }

    @Test
    public void testEchoStringHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.EchoStringHandler",
            "build/lib/echo-string-handler-1.0-SNAPSHOT.jar"
        );
        String payload = "abcdefg";
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals(payload, result);
    }

    @Test
    public void testConsumerHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.ConsumerHandler",
            "build/lib/consumer-handler-1.0-SNAPSHOT.jar"
        );
        String payload = "abcdefg";
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals("", result);
    }

    @Test
    public void testRunnableHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.RunnableHandler",
            "build/lib/runnable-handler-1.0-SNAPSHOT.jar"
        );
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, "");

        assertEquals("", result);
    }

    @Test
    public void testReturnNullHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.ReturnNullHandler",
            "build/lib/return-null-handler-1.0-SNAPSHOT.jar"
        );
        String payload = "abcdefg";
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals("null", result);
    }

    @Test
    public void testPojoHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.PojoHandler",
            "build/lib/pojo-handler-1.0-SNAPSHOT.jar"
        );
        String payload = "{ \"name\": \"John\", \"age\": 23 }";
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals("{\"name\":\"Dublin\",\"yearFounded\":841}", result);
    }

    @Test
    public void testPojoHandler2() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.PojoHandler2",
            "build/lib/pojo-handler2-1.0-SNAPSHOT.jar"
        );
        String payload = "{ \"name\": \"John\", \"age\": 23 }";
        String result = ColdLambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals("{\"name\":\"Dublin\",\"yearFounded\":841}", result);
    }

    @Test
    public void testMultipleHandlersWithConfig() {
        // Example of how HandlerConfig can be used to organize handler configurations
        HandlerConfig echoConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.EchoStringHandler",
            "build/lib/echo-string-handler-1.0-SNAPSHOT.jar"
        );
        
        HandlerConfig reverseConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.ReverseStringMain",
            "build/lib/reverse-string-handler-1.0-SNAPSHOT.jar"
        );
        
        HandlerConfig pojoConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.PojoHandler",
            "build/lib/pojo-handler-1.0-SNAPSHOT.jar"
        );
        
        // Test echo handler
        String echoResult = ColdLambdaContainer.invokeLambda(echoConfig, "test");
        assertEquals("test", echoResult);
        
        // Test reverse handler
        String reverseResult = ColdLambdaContainer.invokeLambda(reverseConfig, "hello");
        assertEquals("olleh", reverseResult);
        
        // Test pojo handler
        String pojoResult = ColdLambdaContainer.invokeLambda(pojoConfig, "{ \"name\": \"John\", \"age\": 23 }");
        assertEquals("{\"name\":\"Dublin\",\"yearFounded\":841}", pojoResult);
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