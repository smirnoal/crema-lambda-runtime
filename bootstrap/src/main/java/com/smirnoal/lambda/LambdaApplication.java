package com.smirnoal.lambda;


import com.smirnoal.lambda.Lambda.Environment;
import com.smirnoal.lambda.Lambda.SnapStart;
import com.smirnoal.lambda.log.RicLog;
import com.smirnoal.lambda.log.RicLog.RicLogger;
import com.smirnoal.lambda.log.TelemetryLogRedirection;
import com.smirnoal.lambda.rapid.client.*;
import com.smirnoal.lambda.rapid.client.converters.ErrorRequestConverter;
import com.smirnoal.lambda.rapid.client.converters.XRayErrorCauseConverter;
import com.smirnoal.lambda.rapid.client.dto.ErrorRequest;
import com.smirnoal.lambda.rapid.client.dto.InvocationRequest;
import com.smirnoal.lambda.rapid.client.dto.XRayErrorCause;

import java.io.IOException;
import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import static com.smirnoal.lambda.Lambda.Constants.ERROR_TYPE_AFTER_RESTORE;
import static com.smirnoal.lambda.Lambda.Constants.ERROR_TYPE_BEFORE_SNAPSHOT;
import static com.smirnoal.lambda.Lambda.Constants.INITIALIZATION_TYPE_SNAP_START;
import static com.smirnoal.lambda.Lambda.Constants.LAMBDA_TRACE_HEADER_PROP;


public class LambdaApplication {

    private static final RicLogger log = RicLog.getLogger("main");

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
        log.log(() -> "Runtime API client: " + runtimeApiClient.getClass().getName());
        TelemetryLogRedirection.setupIfAvailable();
    }

    public void run(Runnable runnable) {
        LambdaHandler<Void, Void> handler = new LambdaHandler<Void, Void>()
                .withHandler(runnable);
        run(handler);
    }

    public <T, R> void run(LambdaHandler<T, R> lambdaHandler) {
        if (isSnapStart()) {
            runSnapStartHooks();
        }
        while (true) {
            log.log("polling next()");
            InvocationRequest request = runtimeApiClient.next();
            log.log(() -> "got request id=" + request.id() + " contentLen=" + (request.content() != null ? request.content().length : 0));
            setXrayTraceId(request.xrayTraceId());
            InvocationContext ctx = new InvocationContext(request);
            Lambda.setCurrentContext(ctx);

            try {
                log.log("handler processing");
                T inputEvent = lambdaHandler.toInputType(request.content());
                R result = lambdaHandler.handle(inputEvent);
                byte[] bytes = lambdaHandler.toBytes(result);
                log.log(() -> "reportInvocationSuccess len=" + (bytes != null ? bytes.length : 0));
                runtimeApiClient.reportInvocationSuccess(request.id(), bytes);
                log.log("done");
            } catch (Throwable t) {
                log.log(() -> "handler threw: " + t.getClass().getSimpleName() + ": " + t.getMessage()
                        + (t.getCause() != null ? " | cause: " + t.getCause().getClass().getSimpleName() + ": " + t.getCause().getMessage() : ""));
                ErrorRequest errorRequest = ErrorRequestConverter.fromThrowable(t);
                XRayErrorCause xRayErrorCause = XRayErrorCauseConverter.fromThrowable(t);
                LambdaError lambdaError = new LambdaError(errorRequest, xRayErrorCause);
                runtimeApiClient.reportInvocationError(request.id(), lambdaError);
            } finally {
                Lambda.setCurrentContext(null);
            }
        }
    }

    public <T> void run(LambdaStreamingHandler<T> streamingHandler) {
        if (isSnapStart()) {
            runSnapStartHooks();
        }
        while (true) {
            log.log("streaming polling next()");
            InvocationRequest request = runtimeApiClient.next();
            log.log(() -> "streaming got request id=" + request.id() + " contentLen=" + (request.content() != null ? request.content().length : 0));
            setXrayTraceId(request.xrayTraceId());
            InvocationContext ctx = new InvocationContext(request);
            Lambda.setCurrentContext(ctx);

            log.log("streaming startStreamingResponse");
            StreamingResponseHandle handle =
                    runtimeApiClient.startStreamingResponse(request.id());
            try {
                log.log("streaming handler processing");
                T inputEvent = streamingHandler.toInputType(request.content());
                streamingHandler.handle(inputEvent, handle.responseStream());
                log.log("streaming handle.complete()");
                handle.complete();
                log.log("streaming done");
            } catch (Throwable t) {
                log.log(() -> "streaming handler threw: " + t.getClass().getSimpleName() + ": " + t.getMessage()
                        + (t.getCause() != null ? " | cause: " + t.getCause().getClass().getSimpleName() + ": " + t.getCause().getMessage() : ""));
                ErrorRequest errorRequest = ErrorRequestConverter.fromThrowable(t);
                XRayErrorCause xRayErrorCause = XRayErrorCauseConverter.fromThrowable(t);
                try {
                    handle.fail(new LambdaError(errorRequest, xRayErrorCause));
                } catch (IOException e) {
                    throw new LambdaRapidClientException("Failed to report streaming error", e);
                }
            } finally {
                Lambda.setCurrentContext(null);
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

    private boolean isSnapStart() {
        return INITIALIZATION_TYPE_SNAP_START.equalsIgnoreCase(Environment.AWS_LAMBDA_INITIALIZATION_TYPE);
    }

    private void runSnapStartHooks() {
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
