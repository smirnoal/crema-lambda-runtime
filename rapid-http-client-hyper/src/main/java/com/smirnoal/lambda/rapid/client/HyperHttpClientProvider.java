package com.smirnoal.lambda.rapid.client;

public final class HyperHttpClientProvider implements LambdaRapidHttpClientProvider {

    @Override
    public LambdaRapidHttpClient create(String runtimeApiHost) {
        return new HyperLambdaRapidHttpClient(runtimeApiHost);
    }

    @Override
    public int priority() {
        return 15;
    }
}
