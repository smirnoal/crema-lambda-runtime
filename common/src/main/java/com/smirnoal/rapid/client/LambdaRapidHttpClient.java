package com.smirnoal.rapid.client;


import com.smirnoal.rapid.client.dto.InvocationRequest;

public interface LambdaRapidHttpClient {

    void initError(LambdaError error);

    InvocationRequest next();

    void reportInvocationSuccess(String requestId, byte[] response);

    void reportInvocationError(String requestId, LambdaError error);

    void restoreNext();

    void reportRestoreError(LambdaError error);
}
