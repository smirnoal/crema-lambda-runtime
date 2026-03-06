package com.smirnoal.lambda.rapid.client;

import com.smirnoal.lambda.rapid.client.dto.InvocationRequest;
import com.smirnoal.lambda.rapid.client.serde.JsonSerializer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.smirnoal.lambda.rapid.client.RuntimeApiConstants.*;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_OK;

final class OkHttpLambdaRapidHttpClient implements LambdaRapidHttpClient {

    static final String USER_AGENT =
            "com-smirnoal-okhttp/%s".formatted(System.getProperty("java.vendor.version"));

    private static final MediaType JSON = MediaType.get("application/json");
    private static final MediaType OCTET_STREAM = MediaType.get("application/octet-stream");

    private volatile OkHttpClient httpClient;
    private final Request nextRequest;
    private final String baseUrl;
    private final String invocationEndpoint;

    OkHttpLambdaRapidHttpClient(String runtimeApiHost) {
        Objects.requireNonNull(runtimeApiHost, "host cannot be null");
        this.baseUrl = "http://" + runtimeApiHost;
        this.invocationEndpoint = this.baseUrl + PATH_INVOCATION;

        String nextRequestEndpoint = this.invocationEndpoint + "next";
        nextRequest = new Request.Builder()
                .url(nextRequestEndpoint)
                .header("User-Agent", USER_AGENT)
                .get()
                .build();
    }

    private OkHttpClient httpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                            .connectTimeout(REQUEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                            .readTimeout(REQUEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                            .writeTimeout(REQUEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                            .followRedirects(false)
                            .retryOnConnectionFailure(false)
                            .build();
                }
            }
        }
        return httpClient;
    }

    @Override
    public void initError(LambdaError error) {
        reportLambdaError(this.baseUrl + PATH_INIT_ERROR, error);
    }

    @Override
    public InvocationRequest next() {
        try (Response response = httpClient().newCall(nextRequest).execute()) {
            String requestId = response.header(HEADER_AWS_REQUEST_ID);
            if (requestId == null) {
                throw new LambdaRapidClientException("Request ID absent");
            }
            String invokedFunctionArn = response.header(HEADER_INVOKED_FUNCTION_ARN);
            if (invokedFunctionArn == null) {
                throw new LambdaRapidClientException("Function ARN absent");
            }
            String deadlineStr = response.header(HEADER_DEADLINE_MS);
            long deadlineTimeInMs = deadlineStr != null ? Long.parseLong(deadlineStr) : 0L;
            String xrayTraceId = response.header(HEADER_TRACE_ID);
            String clientContext = response.header(HEADER_CLIENT_CONTEXT);
            String cognitoIdentity = response.header(HEADER_COGNITO_IDENTITY);
            byte[] content = response.body() != null ? response.body().bytes() : new byte[0];

            return InvocationRequest.builder()
                    .withId(requestId)
                    .withInvokedFunctionArn(invokedFunctionArn)
                    .withDeadlineTimeInMs(deadlineTimeInMs)
                    .withXrayTraceId(xrayTraceId)
                    .withClientContext(clientContext)
                    .withCognitoIdentity(cognitoIdentity)
                    .withContent(content)
                    .build();
        } catch (LambdaRapidClientException e) {
            throw e;
        } catch (Exception e) {
            throw new LambdaRapidClientException("Failed to get next invoke", e);
        }
    }

    @Override
    public void reportInvocationSuccess(String requestId, byte[] response) {
        String endpoint = this.invocationEndpoint + requestId + "/response";
        Request request = new Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .post(RequestBody.create(response, OCTET_STREAM))
                .build();

        try (Response resp = httpClient().newCall(request).execute()) {
            // response discarded
        } catch (Exception e) {
            throw new LambdaRapidClientException("Failed to post invocation result", e);
        }
    }

    @Override
    public void reportInvocationError(String requestId, LambdaError error) {
        String endpoint = this.invocationEndpoint + requestId + "/error";
        reportLambdaError(endpoint, error);
    }

    @Override
    public void restoreNext() {
        String endpoint = this.baseUrl + PATH_RESTORE_NEXT;
        Request request = new Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .get()
                .build();

        try (Response response = httpClient().newCall(request).execute()) {
            if (response.code() != HTTP_OK) {
                throw new LambdaRapidClientException(endpoint, response.code());
            }
        } catch (LambdaRapidClientException e) {
            throw e;
        } catch (Exception e) {
            throw new LambdaRapidClientException(endpoint, e);
        }
    }

    @Override
    public void reportRestoreError(LambdaError error) {
        reportLambdaError(this.baseUrl + PATH_RESTORE_ERROR, error);
    }

    private void reportLambdaError(String endpoint, LambdaError error) {
        Request.Builder request = new Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json");

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
        request.post(RequestBody.create(payload, JSON));

        try (Response response = httpClient().newCall(request.build()).execute()) {
            if (response.code() != HTTP_ACCEPTED) {
                throw new LambdaRapidClientException(endpoint, response.code());
            }
        } catch (IOException e) {
            throw new LambdaRapidClientException("Failed to post error", e);
        }
    }
}
