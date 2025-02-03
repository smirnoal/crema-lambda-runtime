package com.smirnoal.rapid.client;


import com.smirnoal.rapid.client.dto.ErrorRequest;
import com.smirnoal.rapid.client.dto.XRayErrorCause;

public class LambdaError {

    public final ErrorRequest errorRequest;

    public final XRayErrorCause xRayErrorCause;

    public LambdaError(ErrorRequest errorRequest, XRayErrorCause xRayErrorCause) {
        this.errorRequest = errorRequest;
        this.xRayErrorCause = xRayErrorCause;
    }

    public LambdaError(ErrorRequest errorRequest) {
        this(errorRequest, null);
    }
}
