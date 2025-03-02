package com.smirnoal.lambda.rapid.client;


import com.smirnoal.lambda.rapid.client.dto.ErrorRequest;
import com.smirnoal.lambda.rapid.client.dto.XRayErrorCause;

public class LambdaError {

    public ErrorRequest errorRequest;

    public XRayErrorCause xRayErrorCause;

    public LambdaError(ErrorRequest errorRequest, XRayErrorCause xRayErrorCause) {
        this.errorRequest = errorRequest;
        this.xRayErrorCause = xRayErrorCause;
    }

    public LambdaError(ErrorRequest errorRequest) {
        this(errorRequest, null);
    }

    public LambdaError withXRayErrorCause(XRayErrorCause xRayErrorCause) {
        this.xRayErrorCause = xRayErrorCause;
        return this;
    }

    @Override
    public String toString() {
        return "LambdaError{" +
                "errorRequest=" + errorRequest +
                ", xRayErrorCause=" + xRayErrorCause +
                '}';
    }
}
