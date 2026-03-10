package com.smirnoal.lambda;

import com.smirnoal.lambda.rapid.client.LambdaRapidHttpClient;
import com.smirnoal.lambda.rapid.client.LambdaRapidHttpClientProvider;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Test-only provider with high priority so that {@link LambdaApplication}'s
 * default constructor picks up a controllable mock client.
 */
public final class MockSnapStartLambdaRapidHttpClientProvider implements LambdaRapidHttpClientProvider {
    static final AtomicReference<LambdaRapidHttpClient> CLIENT = new AtomicReference<>();

    @Override
    public LambdaRapidHttpClient create(String runtimeApiHost) {
        LambdaRapidHttpClient client = CLIENT.get();
        if (client == null) {
            throw new IllegalStateException("Mock LambdaRapidHttpClient not configured");
        }
        return client;
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    static void withClient(LambdaRapidHttpClient client, Runnable body) {
        CLIENT.set(client);
        try {
            body.run();
        } finally {
            CLIENT.set(null);
        }
    }
}

