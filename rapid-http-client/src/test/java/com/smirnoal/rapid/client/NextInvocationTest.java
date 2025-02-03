package com.smirnoal.rapid.client;

import com.smirnoal.rapid.client.dto.InvocationRequest;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.CoreMatchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class NextInvocationTest {
    MockWebServer mockWebServer;
    LambdaRapidHttpClientImpl runtimeClient;

    String requestId;
    final String functionArn = "arn:aws:lambda:us-east-1:010203040506:function:java-ric-test";
    final String body = "{ \"foo\": \"bar\" }";
    final long defaultDeadlineTime = 0L;

    MockResponse response = new MockResponse();

    @BeforeEach
    void setUp() {
        mockWebServer = new MockWebServer();
        String hostnamePort = getHostnamePort();
        runtimeClient = new LambdaRapidHttpClientImpl(hostnamePort);

        requestId = UUID.randomUUID().toString();

        response.addHeader("lambda-runtime-aws-request-id", requestId);
        response.addHeader("lambda-runtime-invoked-function-arn", functionArn);
        response.setBody(body);
    }

    @NotNull
    private String getHostnamePort() {
        return mockWebServer.getHostName() + ":" + mockWebServer.getPort();
    }

    @Test
    void waitForNextInvocation_basic() throws InterruptedException {
        mockWebServer.enqueue(response);

        InvocationRequest result = runtimeClient.next();
        assertEquals(requestId, result.id);
        assertEquals(functionArn, result.invokedFunctionArn);
        assertEquals(defaultDeadlineTime, result.deadlineTimeInMs);
        assertNull(result.xrayTraceId);
        assertNull(result.clientContext);
        assertNull(result.cognitoIdentity);
        assertEquals(body, new String(result.content));

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        HttpUrl actualUrl = recordedRequest.getRequestUrl();
        assertNotNull(actualUrl);
        String expectedUrl = "http://" + getHostnamePort() + "/2018-06-01/runtime/invocation/next";
        assertEquals(expectedUrl, actualUrl.toString());

        String userAgent = recordedRequest.getHeader("User-Agent");
        assertThat(userAgent, CoreMatchers.startsWith("com-smirnoal-java/"));
    }

    @Test
    void waitForNextInvocation_delay() {
        int runtimeDeadlineMs = 5000;
        response.addHeader("lambda-runtime-deadline-ms", runtimeDeadlineMs);
        mockWebServer.enqueue(response);

        InvocationRequest result = runtimeClient.next();
        assertEquals(runtimeDeadlineMs, result.deadlineTimeInMs);
    }

    @Test
    void waitForNextInvocation_traceId() {
        String traceId = UUID.randomUUID().toString();
        response.addHeader("lambda-runtime-trace-id", traceId);
        mockWebServer.enqueue(response);

        InvocationRequest result = runtimeClient.next();
        assertEquals(traceId, result.xrayTraceId);
    }

    @Test
    void waitForNextInvocation_clientContext() {
        String clientContext = UUID.randomUUID().toString();
        response.addHeader("lambda-runtime-client-context", clientContext);
        mockWebServer.enqueue(response);

        InvocationRequest result = runtimeClient.next();
        assertEquals(clientContext, result.clientContext);
    }

    @Test
    void waitForNextInvocation_cognito() {
        String cognitoIdentity = UUID.randomUUID().toString();
        response.addHeader("lambda-runtime-cognito-identity", cognitoIdentity);
        mockWebServer.enqueue(response);

        InvocationRequest result = runtimeClient.next();
        assertEquals(cognitoIdentity, result.cognitoIdentity);
    }

    @Test
    void waitForNextInvocation_functionArnAbsent() {
        MockResponse invalidResponse = new MockResponse();
        invalidResponse.addHeader("lambda-runtime-aws-request-id", requestId);
        mockWebServer.enqueue(invalidResponse);

        Exception exception = assertThrows(LambdaRapidClientException.class, () -> runtimeClient.next());
        assertEquals("Function ARN absent", exception.getMessage());
    }

    @Test
    void waitForNextInvocation_requestIdAbsent() {
        MockResponse invalidResponse = new MockResponse();
        invalidResponse.addHeader("lambda-runtime-invoked-function-arn", functionArn);
        mockWebServer.enqueue(invalidResponse);

        Exception exception = assertThrows(LambdaRapidClientException.class, () -> runtimeClient.next());
        assertEquals("Request ID absent", exception.getMessage());
    }
}