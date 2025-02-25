package com.smirnoal.rapid.client;

import com.smirnoal.rapid.client.dto.ErrorRequest;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InitErrorTest extends MockServerBase {

    @Test
    void initError() throws InterruptedException {
        MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(202);
        mockWebServer.enqueue(mockResponse);
        String errorMessage = "init error";
        String errorType = "errorType";

        ErrorRequest errorRequest = new ErrorRequest();
        errorRequest.errorMessage = errorMessage;
        errorRequest.errorType = errorType;
        errorRequest.stackTrace = new String[]{"stack trace line 1", "stack trace line 2"};

        LambdaError lambdaError = new LambdaError(errorRequest);

        runtimeClient.initError(lambdaError);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        HttpUrl actualUrl = recordedRequest.getRequestUrl();
        assertNotNull(actualUrl);
        String expectedUrl = "http://" + getHostnamePort() + "/2018-06-01/runtime/init/error";
        assertEquals(expectedUrl, actualUrl.toString());

        String userAgent = recordedRequest.getHeader("User-Agent");
        assertThat(userAgent, CoreMatchers.startsWith(EXPECTED_USER_AGENT));

        String contentType = recordedRequest.getHeader("Content-Type");
        assertEquals(contentType, "application/json");

        String actualErrorType = recordedRequest.getHeader("Lambda-Runtime-Function-Error-Type");
        assertEquals(errorType, actualErrorType);
    }
}