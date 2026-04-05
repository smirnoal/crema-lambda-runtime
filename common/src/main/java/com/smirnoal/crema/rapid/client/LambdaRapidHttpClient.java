package com.smirnoal.crema.rapid.client;


import com.smirnoal.crema.rapid.client.dto.InvocationRequest;

/**
 * HTTP client for the <a href="https://docs.aws.amazon.com/lambda/latest/dg/runtimes-api.html">Lambda Runtime API</a>.
 * Communicates with the Lambda service at {@code AWS_LAMBDA_RUNTIME_API}/2018-06-01.
 */
public interface LambdaRapidHttpClient {

    /**
     * Reports initialization failure to {@code /runtime/init/error}.
     * Call when the runtime cannot load the function (e.g. handler not found, config invalid).
     *
     * @param error the error details (message, type, stack trace)
     */
    void initError(LambdaError error);

    /**
     * Polls {@code /runtime/invocation/next} for the next invocation event.
     * Long-polling; response includes payload and headers (request ID, deadline, ARN, trace ID).
     *
     * @return the invocation request containing payload and metadata headers
     */
    InvocationRequest next();

    /**
     * Posts function result to {@code /runtime/invocation/{requestId}/response}.
     *
     * @param requestId the request ID from the invocation (Lambda-Runtime-Aws-Request-Id)
     * @param response  the function output payload to return to the client
     */
    void reportInvocationSuccess(String requestId, byte[] response);

    /**
     * Reports invocation failure to {@code /runtime/invocation/{requestId}/error}.
     *
     * @param requestId the request ID from the invocation
     * @param error     the error details to report to Lambda
     */
    void reportInvocationError(String requestId, LambdaError error);

    /**
     * Polls {@code /runtime/restore/next} (SnapStart restore phase).
     * Signals readiness after restoring from a snapshot.
     */
    void restoreNext();

    /**
     * Reports restore failure to {@code /runtime/restore/error} (SnapStart).
     *
     * @param error the error details for the restore failure
     */
    void reportRestoreError(LambdaError error);

    /**
     * Starts a streaming response for the invocation. Uses {@code POST /runtime/invocation/{requestId}/response}
     * with {@code Lambda-Runtime-Function-Response-Mode: streaming} and chunked transfer encoding.
     * Default implementation throws UnsupportedOperationException.
     *
     * @param requestId the invocation request ID
     * @return handle with response stream and complete/fail lifecycle methods
     */
    default StreamingResponseHandle startStreamingResponse(String requestId) {
        throw new UnsupportedOperationException(
                "This HTTP client does not support streaming responses. "
                        + "Use rapid-client-netty for streaming support.");
    }
}
