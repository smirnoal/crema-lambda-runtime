package com.smirnoal.lambda;


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

import java.util.Comparator;
import java.util.ServiceLoader;

import static com.smirnoal.lambda.Lambda.Constants.LAMBDA_TRACE_HEADER_PROP;


public class LambdaApplication {

    private final LambdaRapidHttpClient runtimeApiClient;

    public LambdaApplication() {
        String host = Lambda.Environment.AWS_LAMBDA_RUNTIME_API;
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
                } catch (java.io.IOException e) {
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
}
