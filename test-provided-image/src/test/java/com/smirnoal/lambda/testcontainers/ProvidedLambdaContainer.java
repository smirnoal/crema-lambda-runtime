package com.smirnoal.lambda.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProvidedLambdaContainer implements AutoCloseable {
    final Logger logger = LoggerFactory.getLogger(ProvidedLambdaContainer.class);
    private static final DockerImageName PROVIDED_IMAGE = DockerImageName.parse("public.ecr.aws/lambda/provided:al2023");
    private static final int CONTAINER_HTTP_PORT = 8080;
    final GenericContainer<?> lambdaContainer;

    public static String invokeLambda(String projectName, String payload) {
        try (var lambdaContainer = new ProvidedLambdaContainer(projectName)) {
            return lambdaContainer.invokeLambda(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke lambda", e);
        }
    }

    private ProvidedLambdaContainer(String projectName) {
        this.lambdaContainer = createContainer(projectName);
        lambdaContainer.start();
        assertTrue(lambdaContainer.isRunning());

        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);
        lambdaContainer.followOutput(logConsumer);
    }

    private static GenericContainer<?> createContainer(String projectName) {
        // Use the copied handler binary in build/handlers/{projectName}/bootstrap
        Path handlerPath = Path.of("build", "handlers", projectName, "bootstrap");
        if (!Files.exists(handlerPath)) {
            throw new RuntimeException("Handler not found at: " + handlerPath + " (current working directory: " + Path.of("").toAbsolutePath() + ")");
        }

        var container = new GenericContainer<>(PROVIDED_IMAGE)
                .withExposedPorts(CONTAINER_HTTP_PORT)
                .withCommand("bootstrap")
                .withCopyFileToContainer(
                    MountableFile.forHostPath(handlerPath),
                    "/var/runtime/bootstrap"
                )
                .waitingFor(Wait.forLogMessage(".*exec '/var/runtime/bootstrap'.*", 1)
                        .withStartupTimeout(Duration.ofSeconds(10)));

        return container;
    }

    String invokeLambda(String payload) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        String hostname = lambdaContainer.getHost();
        Integer port = lambdaContainer.getMappedPort(CONTAINER_HTTP_PORT);

        logger.info("invoke lambda");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://" + hostname + ":" + port + "/2015-03-31/functions/function/invocations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return response.body();
    }

    @Override
    public void close() {
        lambdaContainer.stop();
    }
} 