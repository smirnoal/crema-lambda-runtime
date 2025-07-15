package com.smirnoal.lambda;

import com.smirnoal.lambda.testcontainers.ProvidedLambdaContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseProvidedImageIT {

    @Test
    void testEchoStringHandler() {
        String payload = "abcdefg";
        String response = ProvidedLambdaContainer.invokeLambda("echo-string-handler", payload);
        
        assertEquals(response, payload);
    }

    @Test
    public void testPojoHandler() {
        String payload = "{ \"name\": \"John\", \"age\": 23 }";
        String response = ProvidedLambdaContainer.invokeLambda("pojo-handler", payload);

        assertEquals("{\"name\":\"Dublin\",\"yearFounded\":841}", response);
    }

    @Test
    void testThrowsHandler() {
        String testInput = "Hello, World!";
        String payload = "{\"body\":\"" + testInput + "\"}";
        
        String response = ProvidedLambdaContainer.invokeLambda("throws-handler", payload);
        
        assertTrue(response.contains("exception message"), 
                "Response should contain the exception message: " + response);
        assertTrue(response.contains("MySpecialException"), 
                "Response should contain the exception type: " + response);
    }
} 