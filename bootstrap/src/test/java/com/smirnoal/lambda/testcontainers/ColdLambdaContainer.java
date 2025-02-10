package com.smirnoal.lambda.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

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

public class ColdLambdaContainer implements AutoCloseable {

    private static final DockerImageName JAVA_17_LAMBDA_IMAGE = DockerImageName.parse("public.ecr.aws/lambda/java:17");
    final GenericContainer<?> lambdaContainer;


    public static String invokeLambda(String handler, String jsonPayload) {
        try (var lambdaContainer = new ColdLambdaContainer(handler)) {
            return lambdaContainer.invokeLambda(jsonPayload);
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    private ColdLambdaContainer(String handler) {
        this.lambdaContainer = createContainer(handler);
        lambdaContainer.start();
        assertTrue(lambdaContainer.isRunning());
    }

    private static GenericContainer<?> createContainer(String handler) {
        //    https://java.testcontainers.org/features/container_logs/
        var container = new GenericContainer<>(JAVA_17_LAMBDA_IMAGE)
                .withExposedPorts(8080)
                .withCommand(handler)
                .withEnv("AWS_LAMBDA_EXEC_WRAPPER", "/var/task/bootstrap.sh")
//                    .withEnv("_HANDLER", "com.smirnoal.lambda.handlers.Echo")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("target/bootstrap-1.0-SNAPSHOT.jar"),
                        "/var/task/lib/bootstrap-1.0-SNAPSHOT.jar"
                )
                .withCopyFileToContainer(
                        MountableFile.forHostPath("target/lib/rapid-http-client-1.0-SNAPSHOT.jar"),
                        "/var/task/lib/rapid-http-client-1.0-SNAPSHOT.jar"
                )
                .withCopyFileToContainer(
                        MountableFile.forHostPath("target/test-classes/"),
                        "/var/task/"
                )
                .waitingFor(Wait.forLogMessage(".*exec '/var/runtime/bootstrap'.*", 1)
                        .withStartupTimeout(Duration.ofSeconds(10)));
        return container;
    }

    String invokeLambda(String jsonPayload) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        String hostname = lambdaContainer.getHost();
        Integer port = lambdaContainer.getMappedPort(8080);

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
    public void close() throws Exception {
        lambdaContainer.stop();
    }
}
