package com.smirnoal.lambda;


import com.smirnoal.rapid.client.LambdaRapidHttpClientImpl;
import com.smirnoal.rapid.client.dto.InvocationRequest;

import java.util.Objects;

import static com.smirnoal.lambda.Lambda.Constants.LAMBDA_TRACE_HEADER_PROP;


public class LambdaApplication {

    private LambdaRapidHttpClientImpl runtimeApiClient;
    private LambdaHandler handler;

    public LambdaApplication() {
    }

    public LambdaApplication withHandler(LambdaHandler lambdaHandler) {
        this.handler = lambdaHandler;
        return this;
    }

    public LambdaApplication withRuntimeApiClient(LambdaRapidHttpClientImpl runtimeApiClient) {
        this.runtimeApiClient = runtimeApiClient;
        return this;
    }

    public void run() {

        Objects.requireNonNull(handler, "Please specify handler");

        if (runtimeApiClient == null) {
            runtimeApiClient = new LambdaRapidHttpClientImpl(
                    Lambda.Environment.AWS_LAMBDA_RUNTIME_API);
        }

        while (true) {
            InvocationRequest request = runtimeApiClient.next();
            setXrayTraceId(request.xrayTraceId);
            Lambda.context = new EventContext(request);

            byte[] result;
            try {
                result = handler.handle(request.content);
                runtimeApiClient.reportInvocationSuccess(request.id, result);
            } catch (Throwable t) {
                //
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
