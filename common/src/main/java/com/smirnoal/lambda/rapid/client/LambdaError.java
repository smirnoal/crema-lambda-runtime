package com.smirnoal.lambda.rapid.client;


import com.smirnoal.lambda.rapid.client.dto.ErrorRequest;
import com.smirnoal.lambda.rapid.client.dto.XRayErrorCause;

public record LambdaError(
        ErrorRequest errorRequest,
        XRayErrorCause xRayErrorCause) {

    public LambdaError(ErrorRequest errorRequest) {
        this(errorRequest, null);
    }
}
