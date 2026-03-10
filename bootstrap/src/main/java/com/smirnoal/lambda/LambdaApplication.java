package com.smirnoal.lambda;


import com.smirnoal.lambda.Lambda.Environment;
import com.smirnoal.lambda.Lambda.SnapStart;
import com.smirnoal.lambda.rapid.client.LambdaError;
import com.smirnoal.lambda.rapid.client.LambdaRapidClientException;
import com.smirnoal.lambda.rapid.client.LambdaRapidHttpClient;
import com.smirnoal.lambda.rapid.client.LambdaRapidHttpClientProvider;
import com.smirnoal.lambda.rapid.client.StreamingResponseHandle;
import com.smirnoal.lambda.rapid.client.converters.ErrorRequestConverter;
import com.smirnoal.lambda.rapid.client.converters.XRayErrorCauseConverter;
import com.smirnoal.lambda.rapid.client.dto.ErrorRequest;
import com.smirnoal.lambda.rapid.client.dto.InvocationRequest;
import com.smirnoal.lambda.rapid.client.dto.XRayErrorCause;
import com.smirnoal.lambda.log.TelemetryLogRedirection;

import java.io.IOException;
import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import static com.smirnoal.lambda.Lambda.Constants.ERROR_TYPE_AFTER_RESTORE;
import static com.smirnoal.lambda.Lambda.Constants.ERROR_TYPE_BEFORE_SNAPSHOT;
import static com.smirnoal.lambda.Lambda.Constants.INITIALIZATION_TYPE_SNAP_START;
import static com.smirnoal.lambda.Lambda.Constants.LAMBDA_TRACE_HEADER_PROP;


public class LambdaApplication {

    private final LambdaRapidHttpClient runtimeApiClient;

    public LambdaApplication() {
        String host = Environment.AWS_LAMBDA_RUNTIME_API;
        this.runtimeApiClient = ServiceLoader.load(LambdaRapidHttpClientProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .max(Comparator.comparingInt(LambdaRapidHttpClientProvider::priority))
                .map(p -> p.create(host))
                .orElseThrow(() -> new IllegalStateException(
                        "No LambdaRapidHttpClientProvider found on classpath"));
        TelemetryLogRedirection.setupIfAvailable();
    }

    public void run(Runnable runnable) {
        LambdaHandler<Void, Void> handler = new LambdaHandler<Void, Void>()
                .withHandler(runnable);
        run(handler);
    }

    public <T, R> void run(LambdaHandler<T, R> lambdaHandler) {
        runSnapStartHooks();
        while (true) {
            InvocationRequest request = runtimeApiClient.next();
            setXrayTraceId(request.xrayTraceId());
            Lambda.invocationRequest = request;

            try {
                T inputEvent = lambdaHandler.toInputType(request.content());
                R result = lambdaHandler.handle(inputEvent);
                byte[] bytes = lambdaHandler.toBytes(result);
                runtimeApiClient.reportInvocationSuccess(request.id(), bytes);
            } catch (Throwable t) {
                ErrorRequest errorRequest = ErrorRequestConverter.fromThrowable(t);
                XRayErrorCause xRayErrorCause = XRayErrorCauseConverter.fromThrowable(t);
                LambdaError lambdaError = new LambdaError(errorRequest, xRayErrorCause);
                runtimeApiClient.reportInvocationError(request.id(), lambdaError);
            }
        }
    }

    public <T> void run(LambdaStreamingHandler<T> streamingHandler) {
        runSnapStartHooks();
        while (true) {
            InvocationRequest request = runtimeApiClient.next();
            setXrayTraceId(request.xrayTraceId());
            Lambda.invocationRequest = request;

            StreamingResponseHandle handle =
                    runtimeApiClient.startStreamingResponse(request.id());
            try {
                T inputEvent = streamingHandler.toInputType(request.content());
                streamingHandler.handle(inputEvent, handle.responseStream());
                handle.complete();
            } catch (Throwable t) {
                ErrorRequest errorRequest = ErrorRequestConverter.fromThrowable(t);
                XRayErrorCause xRayErrorCause = XRayErrorCauseConverter.fromThrowable(t);
                try {
                    handle.fail(new LambdaError(errorRequest, xRayErrorCause));
                } catch (IOException e) {
                    throw new LambdaRapidClientException("Failed to report streaming error", e);
                }
            }
        }
    }

    private static void setXrayTraceId(String xrayTraceId) {
        if (xrayTraceId == null) {
            System.clearProperty(LAMBDA_TRACE_HEADER_PROP);
        } else {
            System.setProperty(LAMBDA_TRACE_HEADER_PROP, xrayTraceId);
        }
    }

    private void runSnapStartHooks() {
        if (!INITIALIZATION_TYPE_SNAP_START.equalsIgnoreCase(Environment.AWS_LAMBDA_INITIALIZATION_TYPE)) {
            return;
        }

        try {
            SnapStart.getBeforeSnapshot().forEach(Runnable::run);
            DnsCache.clearInetAddressCache();
            runtimeApiClient.restoreNext();
        } catch (Throwable t) {
            reportErrorSafely(runtimeApiClient::initError, ERROR_TYPE_BEFORE_SNAPSHOT, t);
            throw new IllegalStateException("SnapStart before-checkpoint phase failed", t);
        }

        try {
            SnapStart.getAfterRestore().forEach(Runnable::run);
        } catch (Throwable t) {
            reportErrorSafely(runtimeApiClient::reportRestoreError, ERROR_TYPE_AFTER_RESTORE, t);
            throw new IllegalStateException("SnapStart after-restore phase failed", t);
        }
    }

    private void reportErrorSafely(Consumer<LambdaError> errorReporter, String errorType, Throwable throwable) {
        try {
            LambdaError error = toLambdaError(throwable, errorType);
            errorReporter.accept(error);
        } catch (Throwable reportError) {
            throwable.addSuppressed(reportError);
        }
    }

    private static LambdaError toLambdaError(Throwable t, String errorTypeOverride) {
        ErrorRequest errorRequest = ErrorRequestConverter.fromThrowable(t, errorTypeOverride);
        XRayErrorCause xRayErrorCause = XRayErrorCauseConverter.fromThrowable(t);
        return new LambdaError(errorRequest, xRayErrorCause);
    }
}
