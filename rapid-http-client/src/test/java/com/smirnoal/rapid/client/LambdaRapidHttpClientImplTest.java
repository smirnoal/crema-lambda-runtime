package com.smirnoal.rapid.client;

import com.smirnoal.rapid.client.dto.ErrorRequest;
import com.smirnoal.rapid.client.dto.StackElement;
import com.smirnoal.rapid.client.dto.XRayErrorCause;
import com.smirnoal.rapid.client.dto.XRayException;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.CoreMatchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LambdaRapidHttpClientImplTest {
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
    void postInvocationSuccess() throws InterruptedException {
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

    @Test
    void postInvocationError_noXRay() throws InterruptedException {
        MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(202);
        mockWebServer.enqueue(mockResponse);
        String requestId = UUID.randomUUID().toString();
        String errorMessage = "error message";
        String errorType = "errorType";

        ErrorRequest errorRequest = new ErrorRequest()
                .withErrorMessage(errorMessage)
                .withErrorType(errorType)
                .withStackTrace(new String[]{"stack trace line 1", "stack trace line 2"});

        LambdaError error = new LambdaError(errorRequest);
        runtimeClient.reportInvocationError(requestId, error);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        HttpUrl actualUrl = recordedRequest.getRequestUrl();
        assertNotNull(actualUrl);
        String expectedUrl = "http://" + getHostnamePort() + "/2018-06-01/runtime/invocation/" + requestId + "/error";
        assertEquals(expectedUrl, actualUrl.toString());

        String userAgent = recordedRequest.getHeader("User-Agent");
        assertThat(userAgent, CoreMatchers.startsWith(EXPECTED_USER_AGENT));

        String contentType = recordedRequest.getHeader("Content-Type");
        assertEquals(contentType, "application/json");

        String actualErrorType = recordedRequest.getHeader("Lambda-Runtime-Function-Error-Type");
        assertEquals(errorType, actualErrorType);
    }

    @Test
    void postInvocationError_withXRay() throws InterruptedException {
        MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(202);
        mockWebServer.enqueue(mockResponse);
        String requestId = UUID.randomUUID().toString();
        String errorMessage = "error message";
        String errorType = "errorType";

        ErrorRequest errorRequest = new ErrorRequest()
                .withErrorMessage(errorMessage)
                .withErrorType(errorType)
                .withStackTrace(new String[]{"stack trace line 1", "stack trace line 2"});

        List<XRayException> exceptions = List.of(
                new XRayException("xray_exception_message",
                        "xray_exception_type",
                        List.of(new StackElement("label", "path", 110))
                )
        );

        XRayErrorCause xRayErrorCause = new XRayErrorCause()
                .withWorkingDirectory("/var/task")
                .withExceptions(exceptions)
                .withPaths(List.of("path1", "path2"));

        LambdaError error = new LambdaError(errorRequest)
                .withXRayErrorCause(xRayErrorCause);

        runtimeClient.reportInvocationError(requestId, error);
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        HttpUrl actualUrl = recordedRequest.getRequestUrl();
        assertNotNull(actualUrl);
        String expectedUrl = "http://" + getHostnamePort() + "/2018-06-01/runtime/invocation/" + requestId + "/error";
        assertEquals(expectedUrl, actualUrl.toString());

        String userAgent = recordedRequest.getHeader("User-Agent");
        assertThat(userAgent, CoreMatchers.startsWith(EXPECTED_USER_AGENT));

        String contentType = recordedRequest.getHeader("Content-Type");
        assertEquals(contentType, "application/json");

        String actualErrorType = recordedRequest.getHeader("lambda-runtime-function-error-type");
        assertEquals(errorType, actualErrorType);

        String actualErrorCause = recordedRequest.getHeader("lambda-runtime-function-xray-error-cause");
        String expected = """
                {
                  "working_directory": "/var/task",
                  "exceptions": [
                    {
                      "message": "xray_exception_message",
                      "type": "xray_exception_type",
                      "stack": [
                        {
                          "label": "label",
                          "path": "path",
                          "line": 110
                        }
                      ]
                    }
                  ],
                  "paths": [
                    "path1",
                    "path2"
                  ]
                }
                """
                .replaceAll("\n", "")
                .replaceAll(" ", "");
        assertEquals(expected, actualErrorCause);
    }

    @Test
    void postInvocationError_wrongStatusCode() {
        MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(200);
        mockWebServer.enqueue(mockResponse);
        String requestId = UUID.randomUUID().toString();
        String errorMessage = "error message";
        String errorType = "errorType";

        ErrorRequest errorRequest = new ErrorRequest();
        errorRequest.errorMessage = errorMessage;
        errorRequest.errorType = errorType;
        errorRequest.stackTrace = new String[]{"stack trace line 1", "stack trace line 2"};

        LambdaError lambdaError = new LambdaError(errorRequest);

        Exception exception = assertThrows(LambdaRapidClientException.class,
                () -> runtimeClient.reportInvocationError(requestId, lambdaError));
        String expectedMessage = "http://" + getHostnamePort() +
                "/2018-06-01/runtime/invocation/" + requestId + "/error Response code: '200'.";
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void postInitError() throws InterruptedException {
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