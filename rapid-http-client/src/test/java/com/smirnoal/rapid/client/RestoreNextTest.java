package com.smirnoal.rapid.client;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.CoreMatchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RestoreNextTest {
    MockWebServer mockWebServer;
    LambdaRapidHttpClientImpl runtimeClient;
    private final String EXPECTED_USER_AGENT = "com-smirnoal-java/";

    @BeforeEach
    void setUp() {
        mockWebServer = new MockWebServer();
        String hostnamePort = getHostnamePort();
        runtimeClient = new LambdaRapidHttpClientImpl(hostnamePort);
    }

    @NotNull
    private String getHostnamePort() {
        return mockWebServer.getHostName() + ":" + mockWebServer.getPort();
    }

    @Test
    void restoreNext() throws InterruptedException {
        MockResponse response = new MockResponse();
        mockWebServer.enqueue(response);

        runtimeClient.restoreNext();
        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        HttpUrl actualUrl = recordedRequest.getRequestUrl();
        assertNotNull(actualUrl);
        String expectedUrl = "http://" + getHostnamePort() + "/2018-06-01/runtime/restore/next";
        assertEquals(expectedUrl, actualUrl.toString());

        String userAgent = recordedRequest.getHeader("User-Agent");
        assertThat(userAgent, CoreMatchers.startsWith(EXPECTED_USER_AGENT));
    }

    @Test
    void restoreNext_wrongStatus() throws InterruptedException {
        MockResponse response = new MockResponse();
        response.setResponseCode(202);
        mockWebServer.enqueue(response);

        Exception exception = assertThrows(LambdaRapidClientException.class,
                () -> runtimeClient.restoreNext());
        String expectedMessage = "http://" + getHostnamePort() +
                "/2018-06-01/runtime/restore/next Response code: '202'.";
        assertEquals(expectedMessage, exception.getMessage());
    }
}