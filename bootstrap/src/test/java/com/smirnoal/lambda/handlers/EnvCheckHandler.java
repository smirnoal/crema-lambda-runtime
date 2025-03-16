package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.Lambda;
import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaHandlerBuilder;
import com.smirnoal.lambda.serde.StringSerDe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EnvCheckHandler {
    public static String handle(String event) {

        List<String> result = new ArrayList<>();

        if (!Objects.equals(Lambda.Environment.HANDLER, "com.smirnoal.lambda.handlers.EnvCheckHandler")) {
            result.add("HANDLER is invalid : " + Lambda.Environment.HANDLER);
        }

        if (Lambda.Environment.AWS_EXECUTION_ENV == null) {
            result.add("AWS_EXECUTION_ENV is not defined");
        }

        if (Lambda.Environment.AWS_LAMBDA_FUNCTION_NAME == null) {
            result.add("AWS_LAMBDA_FUNCTION_NAME is not defined");
        }

        if (Lambda.Environment.AWS_LAMBDA_FUNCTION_MEMORY_SIZE == null) {
            result.add("AWS_LAMBDA_FUNCTION_MEMORY_SIZE is not defined");
        }

        if (!Objects.equals(Lambda.Environment.AWS_LAMBDA_FUNCTION_VERSION, "$LATEST")) {
            result.add("AWS_LAMBDA_FUNCTION_VERSION is not defined");
        }

        if (Lambda.Environment.AWS_LAMBDA_LOG_GROUP_NAME == null) {
            result.add("AWS_LAMBDA_LOG_GROUP_NAME is not defined");
        }

        if (Lambda.Environment.AWS_LAMBDA_LOG_STREAM_NAME == null) {
            result.add("AWS_LAMBDA_LOG_STREAM_NAME is not defined");
        }

        if (Lambda.Environment.AWS_LAMBDA_RUNTIME_API == null) {
            result.add("AWS_LAMBDA_RUNTIME_API is not defined");
        }

        if (!Objects.equals(Lambda.Environment.LAMBDA_TASK_ROOT, "/var/task")) {
            result.add("LAMBDA_TASK_ROOT is invalid: " + Lambda.Environment.LAMBDA_TASK_ROOT);
        }

        if (!Objects.equals(Lambda.Environment.LAMBDA_RUNTIME_DIR, "/var/runtime")) {
            result.add("LAMBDA_RUNTIME_DIR is invalid: " + Lambda.Environment.LAMBDA_RUNTIME_DIR);
        }

        if (!Objects.equals(Lambda.Environment.AWS_ACCESS_KEY, "test_aws_access_key")) {
            result.add("AWS_ACCESS_KEY is invalid: " + Lambda.Environment.AWS_ACCESS_KEY);
        }

        if (!Objects.equals(Lambda.Environment.AWS_SECRET_ACCESS_KEY, "test_aws_secret_access_key")) {
            result.add("AWS_SECRET_ACCESS_KEY is invalid: " + Lambda.Environment.AWS_SECRET_ACCESS_KEY);
        }

        if (!Objects.equals(Lambda.Environment.AWS_SESSION_TOKEN, "test_aws_session_token")) {
            result.add("AWS_SESSION_TOKEN is invalid: " + Lambda.Environment.AWS_SESSION_TOKEN);
        }

        if (!Objects.equals(Lambda.Environment.AWS_REGION, "test_aws_region")) {
            result.add("AWS_REGION is invalid: " + Lambda.Environment.AWS_REGION);
        }

        if (!Objects.equals(Lambda.Environment.AWS_DEFAULT_REGION, "test_aws_default_region")) {
            result.add("AWS_DEFAULT_REGION is invalid: " + Lambda.Environment.AWS_DEFAULT_REGION);
        }

        if (!Objects.equals(Lambda.Environment.AWS_LAMBDA_INITIALIZATION_TYPE, "test_aws_lambda_initialization_type")) {
            result.add("AWS_LAMBDA_INITIALIZATION_TYPE is invalid: " + Lambda.Environment.AWS_LAMBDA_INITIALIZATION_TYPE);
        }

        if (!Objects.equals(Lambda.Environment.AWS_ACCESS_KEY_ID, "test_aws_access_key_id")) {
            result.add("AWS_ACCESS_KEY_ID is invalid: " + Lambda.Environment.AWS_ACCESS_KEY_ID);
        }

        return result.stream()
                .collect(Collectors.joining("\n"));
    }

    public static void main(String[] args) {
        LambdaApplication app = new LambdaApplication();

        LambdaHandler<String, String> handler = new LambdaHandlerBuilder<String, String>()
                .withLambdaSerde(new StringSerDe())
                .withHandler(EnvCheckHandler::handle)
                .build();

        app.run(handler);
    }
}
