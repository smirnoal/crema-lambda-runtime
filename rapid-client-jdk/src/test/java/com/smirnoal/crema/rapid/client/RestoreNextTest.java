package com.smirnoal.crema.rapid.client;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RestoreNextTest extends MockServerBase {

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
    void restoreNext_wrongStatus() {
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