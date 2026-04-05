package com.smirnoal.crema.rapid.client;

import okhttp3.mockwebserver.MockWebServer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;

class MockServerBase {
    MockWebServer mockWebServer;
    LambdaRapidHttpClient runtimeClient;
    final String EXPECTED_USER_AGENT = "com.smirnoal.crema-jdk/";

    @BeforeEach
    void initRuntimeClient() {
        mockWebServer = new MockWebServer();
        String hostnamePort = getHostnamePort();
        runtimeClient = new LambdaRapidHttpClientImpl(hostnamePort);
    }

    @NotNull
    String getHostnamePort() {
        return mockWebServer.getHostName() + ":" + mockWebServer.getPort();
    }
}