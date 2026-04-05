package com.smirnoal.crema.rapid.client;

import java.time.Duration;

/**
 * Constants for the <a href="https://docs.aws.amazon.com/lambda/latest/dg/runtimes-api.html">Lambda Runtime API</a>.
 */
public final class RuntimeApiConstants {

    private RuntimeApiConstants() {
    }

    // Path prefix for invocation endpoints.
    public static final String PATH_INVOCATION = "/2018-06-01/runtime/invocation/";

    // Path for reporting initialization failure.
    public static final String PATH_INIT_ERROR = "/2018-06-01/runtime/init/error";

    // Path for SnapStart restore next.
    public static final String PATH_RESTORE_NEXT = "/2018-06-01/runtime/restore/next";

    // Path for reporting restore failure.
    public static final String PATH_RESTORE_ERROR = "/2018-06-01/runtime/restore/error";

    // Response headers (from GET /next)
    public static final String HEADER_AWS_REQUEST_ID = "lambda-runtime-aws-request-id";
    public static final String HEADER_INVOKED_FUNCTION_ARN = "lambda-runtime-invoked-function-arn";
    public static final String HEADER_DEADLINE_MS = "lambda-runtime-deadline-ms";
    public static final String HEADER_TRACE_ID = "lambda-runtime-trace-id";
    public static final String HEADER_CLIENT_CONTEXT = "lambda-runtime-client-context";
    public static final String HEADER_COGNITO_IDENTITY = "lambda-runtime-cognito-identity";

    // Request headers (error reporting)
    public static final String HEADER_ERROR_TYPE = "Lambda-Runtime-Function-Error-Type";
    public static final String HEADER_XRAY_ERROR_CAUSE = "Lambda-Runtime-Function-XRay-Error-Cause";

    // Max size (bytes) for X-Ray error cause in header.
    public static final int XRAY_ERROR_CAUSE_MAX_HEADER_SIZE = 1024 * 1024;

    // Streaming
    public static final String HEADER_RESPONSE_MODE = "Lambda-Runtime-Function-Response-Mode";
    public static final String VALUE_STREAMING = "streaming";
    public static final String TRAILER_ERROR_TYPE = "Lambda-Runtime-Function-Error-Type";
    public static final String TRAILER_ERROR_BODY = "Lambda-Runtime-Function-Error-Body";

    // Request timeout (long-poll /next, SnapStart restore).
    public static final Duration REQUEST_TIMEOUT = Duration.ofDays(14);

    // Lambda sync response payload limit (bytes).
    public static final int MAX_SYNC_RESPONSE_BYTES = 6 * 1024 * 1024;
}
