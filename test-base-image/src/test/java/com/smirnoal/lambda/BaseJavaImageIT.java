package com.smirnoal.lambda;

import com.google.gson.Gson;
import com.smirnoal.lambda.testcontainers.Java17LambdaContainer;
import com.smirnoal.lambda.rapid.client.dto.ErrorRequest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseJavaImageIT {

    @Test
    public void reverseStringTest() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.ReverseStringMain",
            "reverse-string-handler"
        );
        String jsonPayload = "Hello Lambda";
        String expected = new StringBuilder(jsonPayload).reverse().toString();
        String result = Java17LambdaContainer.invokeLambda(handlerConfig, jsonPayload);
        assertEquals(expected, result);
    }

    @Test
    public void throwsTest() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.ThrowsHandler",
            "throws-handler"
        );
        String jsonPayload = "Hello Lambda";
        String result = Java17LambdaContainer.invokeLambda(handlerConfig, jsonPayload);

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
            "throws-cycle-handler"
        );
        String jsonPayload = "Hello Lambda";
        String result = Java17LambdaContainer.invokeLambda(handlerConfig, jsonPayload);

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
            "env-check-handler"
        );
        String result = Java17LambdaContainer.invokeLambda(handlerConfig, "");

        assertEquals("", result);
    }

    @Test
    public void testEchoStringHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.EchoStringHandler",
            "echo-string-handler"
        );

        String payload = "abcdefg";
        String result = Java17LambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals(payload, result);
    }

    @Test
    public void testConsumerHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.ConsumerHandler",
            "consumer-handler"
        );
        String payload = "abcdefg";
        String result = Java17LambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals("", result);
    }

    @Test
    public void testRunnableHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.RunnableHandler",
            "runnable-handler"
        );
        String result = Java17LambdaContainer.invokeLambda(handlerConfig, "");

        assertEquals("", result);
    }

    @Test
    public void testReturnNullHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.ReturnNullHandler",
            "return-null-handler"
        );
        String payload = "abcdefg";
        String result = Java17LambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals("null", result);
    }

    @Test
    public void testPojoHandler() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.PojoHandler",
            "pojo-handler"
        );
        String payload = "{ \"name\": \"John\", \"age\": 23 }";
        String result = Java17LambdaContainer.invokeLambda(handlerConfig, payload);

        assertEquals("{\"name\":\"Dublin\",\"yearFounded\":841}", result);
    }

    @Test
    public void testPojoHandler2() {
        HandlerConfig handlerConfig = new HandlerConfig(
            "com.smirnoal.lambda.handlers.PojoHandler2",
            "pojo-handler2"
        );
        String payload = "{ \"name\": \"John\", \"age\": 23 }";
        String result = Java17LambdaContainer.invokeLambda(handlerConfig, payload);

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
//        String result = BaseimageLambdaContainer.invokeLambda(handler, sb.toString());
//        System.out.println(result.length());
//        assertEquals(sb.toString(), result);
//    }
}