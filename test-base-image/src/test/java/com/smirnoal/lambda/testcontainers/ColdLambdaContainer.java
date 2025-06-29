package com.smirnoal.lambda.testcontainers;

import com.smirnoal.lambda.HandlerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ColdLambdaContainer implements AutoCloseable {
    final Logger logger = LoggerFactory.getLogger(ColdLambdaContainer.class);
    // TODO: make it configurable
    // test candidates: amazoncorretto:17-al2023-headless, amazoncorretto:21-al2023-headless
    // amazonlinux:2023-minimal java24
    private static final DockerImageName JAVA_17_LAMBDA_IMAGE = DockerImageName.parse("public.ecr.aws/lambda/java:17");
    private static final int CONTAINER_HTTP_PORT = 8080;
    private static final String TASK_LIB_DIR = "/var/task/lib/";
    final GenericContainer<?> lambdaContainer;

    public static String invokeLambda(HandlerConfig handlerConfig, String jsonPayload) {
        try (var lambdaContainer = new ColdLambdaContainer(handlerConfig)) {
            return lambdaContainer.invokeLambda(jsonPayload);
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    private ColdLambdaContainer(HandlerConfig handlerConfig) {
        this.lambdaContainer = createContainer(handlerConfig.handlerClassName(), handlerConfig.jarPath());
        lambdaContainer.start();
        assertTrue(lambdaContainer.isRunning());

        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);
        lambdaContainer.followOutput(logConsumer);

//        ToStringConsumer toStringConsumer = new ToStringConsumer();
//        lambdaContainer.followOutput(toStringConsumer, OutputFrame.OutputType.STDOUT);
//        lambdaContainer.followOutput(toStringConsumer, OutputFrame.OutputType.STDERR);
    }

    private static GenericContainer<?> createContainer(String handler, String handlerJarPath) {
        //    https://java.testcontainers.org/features/container_logs/

        var container = new GenericContainer<>(JAVA_17_LAMBDA_IMAGE)
                .withExposedPorts(CONTAINER_HTTP_PORT)
                .withCommand(handler)
                .withEnv("AWS_LAMBDA_EXEC_WRAPPER", "/var/task/bootstrap.sh")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("build/resources/test/bootstrap.sh"),
                        "/var/task/"
                )
                .waitingFor(Wait.forLogMessage(".*exec '/var/runtime/bootstrap'.*", 1)
                        .withStartupTimeout(Duration.ofSeconds(10)));

        configureTestEnvironment(container);
        copyRuntimeLibs(container, handlerJarPath);
        return container;
    }

    private static void configureTestEnvironment(GenericContainer<?> container) {
        container
                .withEnv("AWS_ACCESS_KEY", "test_aws_access_key")
                .withEnv("AWS_ACCESS_KEY_ID", "test_aws_access_key_id")
                .withEnv("AWS_SECRET_ACCESS_KEY", "test_aws_secret_access_key")
                .withEnv("AWS_SESSION_TOKEN", "test_aws_session_token")
                .withEnv("AWS_REGION", "test_aws_region")
                .withEnv("AWS_DEFAULT_REGION", "test_aws_default_region")
                .withEnv("AWS_LAMBDA_INITIALIZATION_TYPE", "test_aws_lambda_initialization_type")
                .withEnv("AWS_ACCESS_KEY_ID", "test_aws_access_key_id");
    }

    private static void copyRuntimeLibs(GenericContainer<?> container, String handlerJarPath) {
        // Always copy bootstrap jar
        container.withCopyFileToContainer(
                MountableFile.forHostPath("build/lib/bootstrap-1.0-SNAPSHOT.jar"),
                TASK_LIB_DIR + "bootstrap-1.0-SNAPSHOT.jar"
        );
        
        // Always copy Gson (needed by some handlers)
        container.withCopyFileToContainer(
                MountableFile.forHostPath("build/lib/gson-2.13.1.jar"),
                TASK_LIB_DIR + "gson-2.13.1.jar"
        );
        
        // Copy error_prone_annotations (transitive dependency of Gson)
        container.withCopyFileToContainer(
                MountableFile.forHostPath("build/lib/error_prone_annotations-2.38.0.jar"),
                TASK_LIB_DIR + "error_prone_annotations-2.38.0.jar"
        );
        
        // Copy specific handler JAR if provided
        if (handlerJarPath != null) {
            String jarFileName = Path.of(handlerJarPath).getFileName().toString();
            container.withCopyFileToContainer(
                    MountableFile.forHostPath(handlerJarPath),
                    TASK_LIB_DIR + jarFileName
            );
        }
    }

    String invokeLambda(String jsonPayload) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        String hostname = lambdaContainer.getHost();
        Integer port = lambdaContainer.getMappedPort(CONTAINER_HTTP_PORT);

        logger.info("invoke lambda");
//        System.out.println(lambdaContainer.getLogs());

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
