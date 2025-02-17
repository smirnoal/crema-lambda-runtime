package com.smirnoal.lambda;

import com.google.gson.Gson;
import com.smirnoal.lambda.testcontainers.ColdLambdaContainer;
import com.smirnoal.rapid.client.dto.ErrorRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LambdaApplicationIT {


    @Test
    public void reverseStringTest() {
        String handler = "com.smirnoal.lambda.handlers.ReverseStringMain";
        String jsonPayload = "Hello Lambda";
        String expected = new StringBuilder(jsonPayload).reverse().toString();
        String result = ColdLambdaContainer.invokeLambda(handler, jsonPayload);
        assertEquals(expected, result);
    }

    @Test
    public void throwsTest() {
        String handler = "com.smirnoal.lambda.handlers.ThrowsHandler";
        String jsonPayload = "Hello Lambda";
        String result = ColdLambdaContainer.invokeLambda(handler, jsonPayload);

        ErrorRequest actualErrorRequest = new Gson().fromJson(result, ErrorRequest.class);
        assertEquals("exception message", actualErrorRequest.errorMessage);
        assertEquals("com.smirnoal.lambda.handlers.ThrowsHandler$MySpecialException", actualErrorRequest.errorType);
        assertEquals("com.smirnoal.lambda.handlers.ThrowsHandler.handle(ThrowsHandler.java:9)", actualErrorRequest.stackTrace[0]);
    }

    @Test
    public void throwsCycleTest() {
        String handler = "com.smirnoal.lambda.handlers.ThrowsCycleHandler";
        String jsonPayload = "Hello Lambda";
        String result = ColdLambdaContainer.invokeLambda(handler, jsonPayload);

        ErrorRequest actualErrorRequest = new Gson().fromJson(result, ErrorRequest.class);
        assertEquals("exception 1", actualErrorRequest.errorMessage);
        assertEquals("java.lang.RuntimeException", actualErrorRequest.errorType);
        assertEquals("com.smirnoal.lambda.handlers.ThrowsCycleHandler.handle(ThrowsCycleHandler.java:9)", actualErrorRequest.stackTrace[0]);
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