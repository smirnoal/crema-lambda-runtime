package com.smirnoal.lambda.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class ColdLambdaContainer implements AutoCloseable {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected static final int CONTAINER_HTTP_PORT = 8080;
    protected GenericContainer<?> lambdaContainer;

    protected ColdLambdaContainer() {
        // Subclasses should call initializeContainer() after they're fully initialized
    }

    protected void initializeContainer() {
        this.lambdaContainer = createContainer();
        lambdaContainer.start();
        assertTrue(lambdaContainer.isRunning());

        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);
        lambdaContainer.followOutput(logConsumer);
    }

    protected abstract GenericContainer<?> createContainer();

    protected void configureTestEnvironment(GenericContainer<?> container) {
        container
                .withEnv("AWS_ACCESS_KEY", "test_aws_access_key")
                .withEnv("AWS_ACCESS_KEY_ID", "test_aws_access_key_id")
                .withEnv("AWS_SECRET_ACCESS_KEY", "test_aws_secret_access_key")
                .withEnv("AWS_SESSION_TOKEN", "test_aws_session_token")
                .withEnv("AWS_REGION", "test_aws_region")
                .withEnv("AWS_DEFAULT_REGION", "test_aws_default_region")
                .withEnv("AWS_LAMBDA_INITIALIZATION_TYPE", "test_aws_lambda_initialization_type");
    }

    protected void configureWaitStrategy(GenericContainer<?> container) {
        container.waitingFor(Wait.forLogMessage(".*exec '/var/runtime/bootstrap'.*", 1)
                .withStartupTimeout(Duration.ofSeconds(10)));
    }

    protected String invokeLambda(String jsonPayload) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        String hostname = lambdaContainer.getHost();
        Integer port = lambdaContainer.getMappedPort(CONTAINER_HTTP_PORT);

        logger.info("invoke lambda");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + hostname + ":" + port + "/2015-03-31/functions/function/invocations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HTTP_OK, response.statusCode());
        return response.body();
    }

    @Override
    public void close() {
        lambdaContainer.stop();
    }
} 