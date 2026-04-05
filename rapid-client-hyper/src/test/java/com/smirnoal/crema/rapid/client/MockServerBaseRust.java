package com.smirnoal.crema.rapid.client;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;

@EnabledOnOs(OS.LINUX)
class MockServerBaseRust {
    MockWebServer mockWebServer;
    LambdaRapidHttpClient runtimeClient;
    static final String EXPECTED_USER_AGENT_PREFIX = "com.smirnoal.crema-hyper/";

    @BeforeEach
    void initRuntimeClient() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String hostnamePort = getHostnamePort();
        runtimeClient = new HyperLambdaRapidHttpClient(hostnamePort);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    String getHostnamePort() {
        return mockWebServer.getHostName() + ":" + mockWebServer.getPort();
    }
}
