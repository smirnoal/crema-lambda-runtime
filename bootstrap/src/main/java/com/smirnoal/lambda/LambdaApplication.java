package com.smirnoal.lambda;


import com.smirnoal.rapid.client.LambdaError;
import com.smirnoal.rapid.client.LambdaRapidHttpClient;
import com.smirnoal.rapid.client.LambdaRapidHttpClientImpl;
import com.smirnoal.rapid.client.converters.ErrorRequestConverter;
import com.smirnoal.rapid.client.converters.XRayErrorCauseConverter;
import com.smirnoal.rapid.client.dto.ErrorRequest;
import com.smirnoal.rapid.client.dto.InvocationRequest;
import com.smirnoal.rapid.client.dto.XRayErrorCause;

import static com.smirnoal.lambda.Lambda.Constants.LAMBDA_TRACE_HEADER_PROP;


public class LambdaApplication {

    private LambdaRapidHttpClient runtimeApiClient;
    private final LambdaHandler handler;

    public LambdaApplication(LambdaHandler lambdaHandler) {
        this.handler = lambdaHandler;
    }

    public LambdaApplication withRuntimeApiClient(LambdaRapidHttpClientImpl runtimeApiClient) {
        this.runtimeApiClient = runtimeApiClient;
        return this;
    }

    public void run() {

        if (runtimeApiClient == null) {
            runtimeApiClient = new LambdaRapidHttpClientImpl(Lambda.Environment.AWS_LAMBDA_RUNTIME_API);
        }

        while (true) {
            InvocationRequest request = runtimeApiClient.next();
            setXrayTraceId(request.xrayTraceId());
            Lambda.invocationRequest = request;

            try {
                byte[] result = handler.handle(request.content());
                runtimeApiClient.reportInvocationSuccess(request.id(), result);
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
