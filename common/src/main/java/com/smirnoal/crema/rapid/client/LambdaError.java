package com.smirnoal.crema.rapid.client;


import com.smirnoal.crema.rapid.client.dto.ErrorRequest;
import com.smirnoal.crema.rapid.client.dto.XRayErrorCause;

public record LambdaError(
        ErrorRequest errorRequest,
        XRayErrorCause xRayErrorCause) {

    public LambdaError(ErrorRequest errorRequest) {
        this(errorRequest, null);
    }
}
