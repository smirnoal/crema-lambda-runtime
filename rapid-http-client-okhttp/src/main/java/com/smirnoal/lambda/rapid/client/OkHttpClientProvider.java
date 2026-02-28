package com.smirnoal.lambda.rapid.client;

public class OkHttpClientProvider implements LambdaRapidHttpClientProvider {
    @Override
    public LambdaRapidHttpClient create(String runtimeApiHost) {
        return new OkHttpLambdaRapidHttpClient(runtimeApiHost);
    }

    @Override
    public int priority() {
        return 10;
    }
}
