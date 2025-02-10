package com.smirnoal.lambda;

import com.smirnoal.lambda.testcontainers.ColdLambdaContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LambdaApplicationIT {

    @Test
    public void echoTest() {
        String handler = "com.smirnoal.lambda.handlers.EchoMain";
        String jsonPayload = "Hello Lambda";
        String result = ColdLambdaContainer.invokeLambda(handler, jsonPayload);
        assertEquals(jsonPayload, result);
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