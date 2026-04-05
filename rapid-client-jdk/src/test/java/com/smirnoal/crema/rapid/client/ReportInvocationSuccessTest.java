package com.smirnoal.crema.rapid.client;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReportInvocationSuccessTest extends MockServerBase {

    @Test
    void reportInvocationSuccess() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse());
        String requestId = UUID.randomUUID().toString();
        String body = "{}";
        runtimeClient.reportInvocationSuccess(requestId, body.getBytes());

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        HttpUrl actualUrl = recordedRequest.getRequestUrl();
        assertNotNull(actualUrl);
        String expectedUrl = "http://" + getHostnamePort() + "/2018-06-01/runtime/invocation/" + requestId + "/response";
        assertEquals(expectedUrl, actualUrl.toString());

        String userAgent = recordedRequest.getHeader("User-Agent");
        assertThat(userAgent, CoreMatchers.startsWith(EXPECTED_USER_AGENT));

        String actualBody = recordedRequest.getBody().readUtf8();
        assertEquals(body, actualBody);
    }
}