package com.smirnoal.lambda.rapid.client;

import okhttp3.mockwebserver.MockWebServer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

class MockServerBase {
    MockWebServer mockWebServer;
    LambdaRapidHttpClient runtimeClient;
    final String EXPECTED_USER_AGENT = "com-smirnoal-okhttp/";

    @BeforeEach
    void initRuntimeClient() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String hostnamePort = getHostnamePort();
        runtimeClient = new OkHttpLambdaRapidHttpClient(hostnamePort);
    }

    @AfterEach
    void shutdownMockServer() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @NotNull
    String getHostnamePort() {
        return mockWebServer.getHostName() + ":" + mockWebServer.getPort();
    }
}
