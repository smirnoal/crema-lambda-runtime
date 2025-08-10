package com.smirnoal.lambda;


import com.smirnoal.lambda.rapid.client.LambdaError;
import com.smirnoal.lambda.rapid.client.LambdaRapidHttpClient;
import com.smirnoal.lambda.rapid.client.LambdaRapidHttpClientImpl;
import com.smirnoal.lambda.rapid.client.converters.ErrorRequestConverter;
import com.smirnoal.lambda.rapid.client.converters.XRayErrorCauseConverter;
import com.smirnoal.lambda.rapid.client.dto.ErrorRequest;
import com.smirnoal.lambda.rapid.client.dto.InvocationRequest;
import com.smirnoal.lambda.rapid.client.dto.XRayErrorCause;
import com.smirnoal.lambda.log.FramedTelemetryLogSink;
import com.smirnoal.lambda.log.TelemetryLogRedirection;

import java.io.FileDescriptor;
import java.io.IOException;

import static com.smirnoal.lambda.Lambda.Constants.LAMBDA_TRACE_HEADER_PROP;


public class LambdaApplication {

    private LambdaRapidHttpClient runtimeApiClient;

    public LambdaApplication withRuntimeApiClient(LambdaRapidHttpClient runtimeApiClient) {
        this.runtimeApiClient = runtimeApiClient;
        return this;
    }

    public void run(Runnable runnable) {
        LambdaHandler<Void, Void> handler = new LambdaHandler<Void, Void>()
                .withHandler(runnable);
        run(handler);
    }

    public <T, R> void run(LambdaHandler<T, R> lambdaHandler) {
        TelemetryLogRedirection.setupIfAvailable();

        if (runtimeApiClient == null) {
            runtimeApiClient = new LambdaRapidHttpClientImpl(Lambda.Environment.AWS_LAMBDA_RUNTIME_API);
        }

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

    private static void setXrayTraceId(String xrayTraceId) {
        if (xrayTraceId == null) {
            System.clearProperty(LAMBDA_TRACE_HEADER_PROP);
        } else {
            System.setProperty(LAMBDA_TRACE_HEADER_PROP, xrayTraceId);
        }
    }
}
