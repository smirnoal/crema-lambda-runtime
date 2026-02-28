package com.smirnoal.lambda.rapid.client;

public class JdkHttpClientProvider implements LambdaRapidHttpClientProvider {
    @Override
    public LambdaRapidHttpClient create(String runtimeApiHost) {
        return new LambdaRapidHttpClientImpl(runtimeApiHost);
    }
}
