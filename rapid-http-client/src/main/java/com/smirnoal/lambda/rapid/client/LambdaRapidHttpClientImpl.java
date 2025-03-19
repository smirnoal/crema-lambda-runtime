package com.smirnoal.lambda.rapid.client;

import com.smirnoal.lambda.rapid.client.dto.InvocationRequest;
import com.smirnoal.lambda.rapid.client.serde.PayloadSerializers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_OK;

public final class LambdaRapidHttpClientImpl implements LambdaRapidHttpClient {

    static final String USER_AGENT =
            "com-smirnoal-java-http/%s".formatted(System.getProperty("java.vendor.version"));

    private static final int XRAY_ERROR_CAUSE_MAX_HEADER_SIZE = 1024 * 1024;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofDays(14))
            .build();

    private final HttpRequest nextRequest;
    private final String baseUrl;
    private final String invocationEndpoint;

    public LambdaRapidHttpClientImpl(String runtimeApiHost) {
        Objects.requireNonNull(runtimeApiHost, "host cannot be null");
        this.baseUrl = "http://" + runtimeApiHost;
        this.invocationEndpoint = this.baseUrl + "/2018-06-01/runtime/invocation/";

        String nextRequestEndpoint = this.invocationEndpoint + "next";
        nextRequest = HttpRequest.newBuilder(URI.create(nextRequestEndpoint))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
    }

    @Override
    public void initError(LambdaError error) {
        URI endpoint = URI.create(this.baseUrl + "/2018-06-01/runtime/init/error");
        reportLambdaError(endpoint, error);
    }

    @Override
    public InvocationRequest next() {
        HttpResponse<byte[]> response;
        try {
            response = HTTP_CLIENT.send(nextRequest, HttpResponse.BodyHandlers.ofByteArray());
        } catch (Exception e) {
            throw new LambdaRapidClientException("Failed to get next invoke", e);
        }

        HttpHeaders headers = response.headers();

        String requestId = headers.firstValue("lambda-runtime-aws-request-id")
                .orElseThrow(() -> new LambdaRapidClientException("Request ID absent"));
        String invokedFunctionArn = headers.firstValue("lambda-runtime-invoked-function-arn")
                .orElseThrow(() -> new LambdaRapidClientException("Function ARN absent"));
        long deadlineTimeInMs = Long.parseLong(
                headers.firstValue("lambda-runtime-deadline-ms").orElse("0")
        );
        String xrayTraceId = headers.firstValue("lambda-runtime-trace-id").orElse(null);
        String clientContext = headers.firstValue("lambda-runtime-client-context").orElse(null);
        String cognitoIdentity = headers.firstValue("lambda-runtime-cognito-identity").orElse(null);
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
                .POST(HttpRequest.BodyPublishers.ofByteArray(response))
                .build();

        try {
            HTTP_CLIENT.send(invocationResponseRequest, HttpResponse.BodyHandlers.discarding());
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
        URI endpoint = URI.create(this.baseUrl + "/2018-06-01/runtime/restore/next");
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .GET()
                .header("User-Agent", USER_AGENT)
                .build();

        HttpResponse<Void> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new LambdaRapidClientException(endpoint.toString(), e);
        }
        if (response.statusCode() != HTTP_OK) {
            throw new LambdaRapidClientException(endpoint.toString(), response.statusCode());
        }
    }

    @Override
    public void reportRestoreError(LambdaError error) {
        URI endpoint = URI.create(this.baseUrl + "/2018-06-01/runtime/restore/error");
        reportLambdaError(endpoint, error);
    }

    void reportLambdaError(URI endpoint, LambdaError error) {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json");

        if (error.xRayErrorCause() != null) {
            byte[] xRayErrorCauseJson = PayloadSerializers.serialize(error.xRayErrorCause());
            if (xRayErrorCauseJson != null && xRayErrorCauseJson.length < XRAY_ERROR_CAUSE_MAX_HEADER_SIZE) {
                request.header("Lambda-Runtime-Function-XRay-Error-Cause", new String(xRayErrorCauseJson));
            }
        }

        if (error.errorRequest().errorType() != null) {
            request.header("Lambda-Runtime-Function-Error-Type", error.errorRequest().errorType());
        }

        byte[] payload = PayloadSerializers.serialize(error.errorRequest());
        request.POST(HttpRequest.BodyPublishers.ofByteArray(payload));

        HttpResponse<Void> response;
        try {
            response = HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.discarding());
        } catch (InterruptedException | IOException e) {
            throw new LambdaRapidClientException("Failed to post error", e);
        }
        if (response.statusCode() != HTTP_ACCEPTED) {
            throw new LambdaRapidClientException(endpoint.toString(), response.statusCode());
        }
    }
}
