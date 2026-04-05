package com.smirnoal.crema.rapid.client;

import com.smirnoal.crema.rapid.client.dto.InvocationRequest;
import com.smirnoal.crema.rapid.client.serde.JsonSerializer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

import static com.smirnoal.crema.rapid.client.RuntimeApiConstants.*;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_OK;

final class LambdaRapidHttpClientImpl implements LambdaRapidHttpClient {

    static final String USER_AGENT =
            "com.smirnoal.crema-jdk/%s".formatted(System.getProperty("java.vendor.version"));

    private volatile HttpClient httpClient;
    private final HttpRequest nextRequest;
    private final String baseUrl;
    private final String invocationEndpoint;

    public LambdaRapidHttpClientImpl(String runtimeApiHost) {
        Objects.requireNonNull(runtimeApiHost, "host cannot be null");
        this.baseUrl = "http://" + runtimeApiHost;
        this.invocationEndpoint = this.baseUrl + PATH_INVOCATION;

        String nextRequestEndpoint = this.invocationEndpoint + "next";
        nextRequest = HttpRequest.newBuilder(URI.create(nextRequestEndpoint))
                .header("User-Agent", USER_AGENT)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
    }

    private HttpClient httpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_1_1)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .connectTimeout(REQUEST_TIMEOUT)
                            .build();
                }
            }
        }
        return httpClient;
    }

    @Override
    public void initError(LambdaError error) {
        reportLambdaError(URI.create(this.baseUrl + PATH_INIT_ERROR), error);
    }

    @Override
    public InvocationRequest next() {
        HttpResponse<byte[]> response;
        try {
            response = httpClient().send(nextRequest, HttpResponse.BodyHandlers.ofByteArray());
        } catch (Exception e) {
            throw new LambdaRapidClientException("Failed to get next invoke", e);
        }

        HttpHeaders headers = response.headers();
        String requestId = headers.firstValue(HEADER_AWS_REQUEST_ID)
                .orElseThrow(() -> new LambdaRapidClientException("Request ID absent"));
        String invokedFunctionArn = headers.firstValue(HEADER_INVOKED_FUNCTION_ARN)
                .orElseThrow(() -> new LambdaRapidClientException("Function ARN absent"));
        long deadlineTimeInMs = Long.parseLong(
                headers.firstValue(HEADER_DEADLINE_MS).orElse("0")
        );
        String xrayTraceId = headers.firstValue(HEADER_TRACE_ID).orElse(null);
        String clientContext = headers.firstValue(HEADER_CLIENT_CONTEXT).orElse(null);
        String cognitoIdentity = headers.firstValue(HEADER_COGNITO_IDENTITY).orElse(null);
        byte[] content = response.body();

        return InvocationRequest.builder()
                .withId(requestId)
                .withInvokedFunctionArn(invokedFunctionArn)
                .withDeadlineTimeInMs(deadlineTimeInMs)
                .withXrayTraceId(xrayTraceId)
                .withClientContext(clientContext)
                .withCognitoIdentity(cognitoIdentity)
                .withContent(content)
                .build();
    }

    @Override
    public void reportInvocationSuccess(String requestId, byte[] response) {
        URI endpoint = URI.create(this.invocationEndpoint + requestId + "/response");
        HttpRequest invocationResponseRequest = HttpRequest.newBuilder(endpoint)
                .header("User-Agent", USER_AGENT)
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofByteArray(response))
                .build();

        try {
            httpClient().send(invocationResponseRequest, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new LambdaRapidClientException("Failed to post invocation result", e);
        }
    }

    @Override
    public void reportInvocationError(String requestId, LambdaError error) {
        URI endpoint = URI.create(this.invocationEndpoint + requestId + "/error");
        reportLambdaError(endpoint, error);
    }

    @Override
    public void restoreNext() {
        URI endpoint = URI.create(this.baseUrl + PATH_RESTORE_NEXT);
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .GET()
                .header("User-Agent", USER_AGENT)
                .timeout(REQUEST_TIMEOUT)
                .build();

        HttpResponse<Void> response;
        try {
            response = httpClient().send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new LambdaRapidClientException(endpoint.toString(), e);
        }
        if (response.statusCode() != HTTP_OK) {
            throw new LambdaRapidClientException(endpoint.toString(), response.statusCode());
        }
    }

    @Override
    public void reportRestoreError(LambdaError error) {
        reportLambdaError(URI.create(this.baseUrl + PATH_RESTORE_ERROR), error);
    }

    void reportLambdaError(URI endpoint, LambdaError error) {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT);

        if (error.xRayErrorCause() != null) {
            byte[] xRayErrorCauseJson = JsonSerializer.serialize(error.xRayErrorCause());
            if (xRayErrorCauseJson.length < XRAY_ERROR_CAUSE_MAX_HEADER_SIZE) {
                request.header(HEADER_XRAY_ERROR_CAUSE, new String(xRayErrorCauseJson));
            }
        }

        if (error.errorRequest().errorType() != null) {
            request.header(HEADER_ERROR_TYPE, error.errorRequest().errorType());
        }

        byte[] payload = JsonSerializer.serialize(error.errorRequest());
        request.POST(HttpRequest.BodyPublishers.ofByteArray(payload));

        HttpResponse<Void> response;
        try {
            response = httpClient().send(request.build(), HttpResponse.BodyHandlers.discarding());
        } catch (InterruptedException | IOException e) {
            throw new LambdaRapidClientException("Failed to post error", e);
        }
        if (response.statusCode() != HTTP_ACCEPTED) {
            throw new LambdaRapidClientException(endpoint.toString(), response.statusCode());
        }
    }
}
