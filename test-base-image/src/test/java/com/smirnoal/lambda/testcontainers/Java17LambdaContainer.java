package com.smirnoal.lambda.testcontainers;

import com.smirnoal.lambda.HandlerConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;

public class Java17LambdaContainer extends ColdLambdaContainer {
    private static final DockerImageName JAVA_17_LAMBDA_IMAGE = DockerImageName.parse("public.ecr.aws/lambda/java:17");
    private static final String TASK_LIB_DIR = "/var/task/lib/";
    private final HandlerConfig handlerConfig;

    public static String invokeLambda(HandlerConfig handlerConfig, String jsonPayload) {
        try (var lambdaContainer = new Java17LambdaContainer(handlerConfig)) {
            return lambdaContainer.invokeLambda(jsonPayload);
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    private Java17LambdaContainer(HandlerConfig handlerConfig) {
        this.handlerConfig = handlerConfig;
        initializeContainer();
    }

    @Override
    protected GenericContainer<?> createContainer() {
        String handler = handlerConfig.handlerClassName();
        String handlerJarPath = handlerConfig.jarPath();

        var container = new GenericContainer<>(JAVA_17_LAMBDA_IMAGE)
                .withExposedPorts(CONTAINER_HTTP_PORT)
                .withCommand(handler)
                .withEnv("AWS_LAMBDA_EXEC_WRAPPER", "/var/task/bootstrap.sh")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("build/resources/test/bootstrap.sh"),
                        "/var/task/"
                );

        configureWaitStrategy(container);
        configureTestEnvironment(container);
        copyRuntimeLibs(container, handlerJarPath);
        return container;
    }

    private void copyRuntimeLibs(GenericContainer<?> container, String handlerJarPath) {
        // Copy specific handler JAR if provided
        if (handlerJarPath != null) {
            String jarFileName = Path.of(handlerJarPath).getFileName().toString();
            container.withCopyFileToContainer(
                    MountableFile.forHostPath(handlerJarPath),
                    TASK_LIB_DIR + jarFileName
            );
        }
    }
}
