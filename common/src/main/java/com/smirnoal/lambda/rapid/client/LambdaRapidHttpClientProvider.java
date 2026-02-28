package com.smirnoal.lambda.rapid.client;

public interface LambdaRapidHttpClientProvider {
    LambdaRapidHttpClient create(String runtimeApiHost);
    default int priority() { return 0; }
}
