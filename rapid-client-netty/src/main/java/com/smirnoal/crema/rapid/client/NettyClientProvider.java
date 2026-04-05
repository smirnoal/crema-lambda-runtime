package com.smirnoal.crema.rapid.client;

public class NettyClientProvider implements LambdaRapidHttpClientProvider {

    @Override
    public LambdaRapidHttpClient create(String runtimeApiHost) {
        return new NettyLambdaRapidHttpClient(runtimeApiHost);
    }

    @Override
    public int priority() {
        return 20;
    }
}
