package com.smirnoal.crema.rapid.client;

public interface LambdaRapidHttpClientProvider {
    LambdaRapidHttpClient create(String runtimeApiHost);
    default int priority() { return 0; }
}
