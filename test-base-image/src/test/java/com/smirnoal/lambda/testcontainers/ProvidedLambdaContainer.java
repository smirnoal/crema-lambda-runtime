package com.smirnoal.lambda.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;

public class ProvidedLambdaContainer extends ColdLambdaContainer {
    private static final DockerImageName PROVIDED_IMAGE = DockerImageName.parse("public.ecr.aws/lambda/provided:al2023");
    private final String projectName;

    public static String invokeLambda(String projectName, String payload) {
        try (var lambdaContainer = new ProvidedLambdaContainer(projectName)) {
            return lambdaContainer.invokeLambda(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke lambda", e);
        }
    }

    private ProvidedLambdaContainer(String projectName) {
        this.projectName = projectName;
        initializeContainer();
    }

    @Override
    protected GenericContainer<?> createContainer() {
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
                );

        configureWaitStrategy(container);
        return container;
    }
} 