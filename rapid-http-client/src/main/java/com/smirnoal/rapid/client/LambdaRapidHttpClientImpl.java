package com.smirnoal.rapid.client;

import com.smirnoal.rapid.client.dto.InvocationRequest;
import com.smirnoal.rapid.client.serde.PayloadSerializers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_OK;

public class LambdaRapidHttpClientImpl {

    static final String USER_AGENT =
            "com-smirnoal-java/%s".formatted(System.getProperty("java.vendor.version"));

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

    public void initError(LambdaError error) throws IOException {
        URI endpoint = URI.create(this.baseUrl + "/2018-06-01/runtime/init/error");
        reportLambdaError(endpoint, error);
    }
    
    public InvocationRequest next() {
        HttpResponse<byte[]> response;
        try {
            response = HTTP_CLIENT.send(nextRequest, HttpResponse.BodyHandlers.ofByteArray());
        } catch (Exception e) {
            throw new LambdaRapidClientException("Failed to get next invoke", e);
        }

        InvocationRequest result = new InvocationRequest();

        result.id = response.headers().firstValue("lambda-runtime-aws-request-id").orElseThrow(
                () -> new LambdaRapidClientException("Request ID absent"));
        result.invokedFunctionArn = response.headers().firstValue("lambda-runtime-invoked-function-arn").orElseThrow(
                () -> new LambdaRapidClientException("Function ARN absent"));
        result.deadlineTimeInMs = Long.parseLong(response.headers().firstValue("lambda-runtime-deadline-ms").orElse("0"));
        result.xrayTraceId = response.headers().firstValue("lambda-runtime-trace-id").orElse(null);
        result.clientContext = response.headers().firstValue("lambda-runtime-client-context").orElse(null);
        result.cognitoIdentity = response.headers().firstValue("lambda-runtime-cognito-identity").orElse(null);
        result.content = response.body();

        return result;
    }
    
    public void reportInvocationSuccess(String requestId, byte[] response) {
        URI endpoint = URI.create(this.invocationEndpoint + requestId + "/response");
        HttpRequest invocationResponseRequest = HttpRequest.newBuilder(endpoint)
                .header("User-Agent", USER_AGENT)
                .POST(HttpRequest.BodyPublishers.ofByteArray(response))
                .build();

        try {
            HTTP_CLIENT.send(invocationResponseRequest, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new RuntimeException("Failed to post invocation result", e);
        }
    }

    public void reportInvocationError(String requestId, LambdaError error) throws IOException {
        URI endpoint = URI.create(this.invocationEndpoint + requestId + "/error");
        reportLambdaError(endpoint, error);
    }
    
    public void restoreNext() throws IOException {
        URI endpoint = URI.create(this.baseUrl + "/2018-06-01/runtime/restore/next");
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .GET()
                .header("User-Agent", USER_AGENT)
                .build();

        HttpResponse<Void> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (response.statusCode() != HTTP_OK) {
            throw new LambdaRapidClientException(endpoint.toString(), response.statusCode());
        }
    }
    
    public void reportRestoreError(LambdaError error) throws IOException {
        URI endpoint = URI.create(this.baseUrl + "/2018-06-01/runtime/restore/error");
        reportLambdaError(endpoint, error);
    }

    void reportLambdaError(URI endpoint, LambdaError error) throws IOException {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json");

        if (error.xRayErrorCause != null) {
            byte[] xRayErrorCauseJson = PayloadSerializers.serialize(error.xRayErrorCause);
            if (xRayErrorCauseJson != null && xRayErrorCauseJson.length < XRAY_ERROR_CAUSE_MAX_HEADER_SIZE) {
                request.header("Lambda-Runtime-Function-XRay-Error-Cause", new String(xRayErrorCauseJson));
            }
        }

        if(error.errorRequest.errorType != null) {
            request.header("Lambda-Runtime-Function-Error-Type", error.errorRequest.errorType);
        }

        byte[] payload = PayloadSerializers.serialize(error.errorRequest);
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
