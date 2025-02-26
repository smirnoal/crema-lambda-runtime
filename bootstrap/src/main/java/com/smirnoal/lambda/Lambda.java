package com.smirnoal.lambda;

import com.smirnoal.rapid.client.dto.InvocationRequest;

public final class Lambda {

    private Lambda() {
    }

    /**
     * <a href="https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html#configuration-envvars-runtime">
     *     Reserved environment variables
     * </a>
     */
    public static final class Environment {
        private Environment() {
        }

        static String HANDLER = System.getenv("_HANDLER");
        static String AWS_DEFAULT_REGION = System.getenv("AWS_DEFAULT_REGION");
        static String AWS_REGION = System.getenv("AWS_REGION");
        static String AWS_EXECUTION_ENV = System.getenv("AWS_EXECUTION_ENV");
        static String AWS_LAMBDA_FUNCTION_NAME = System.getenv("AWS_LAMBDA_FUNCTION_NAME");
        static String AWS_LAMBDA_FUNCTION_MEMORY_SIZE = System.getenv("AWS_LAMBDA_FUNCTION_MEMORY_SIZE");
        static String AWS_LAMBDA_FUNCTION_VERSION = System.getenv("AWS_LAMBDA_FUNCTION_VERSION");
        static String AWS_LAMBDA_INITIALIZATION_TYPE = System.getenv("AWS_LAMBDA_INITIALIZATION_TYPE");
        static String AWS_LAMBDA_LOG_GROUP_NAME = System.getenv("AWS_LAMBDA_LOG_GROUP_NAME");
        static String AWS_LAMBDA_LOG_STREAM_NAME = System.getenv("AWS_LAMBDA_LOG_STREAM_NAME");
        static String AWS_ACCESS_KEY = System.getenv("AWS_ACCESS_KEY");
        static String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
        static String AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
        static String AWS_SESSION_TOKEN = System.getenv("AWS_SESSION_TOKEN");
        static String AWS_LAMBDA_RUNTIME_API = System.getenv("AWS_LAMBDA_RUNTIME_API");
        static String LAMBDA_TASK_ROOT = System.getenv("LAMBDA_TASK_ROOT");
        static String LAMBDA_RUNTIME_DIR = System.getenv("LAMBDA_RUNTIME_DIR");
    }

    public static final class Constants {
        private Constants() {
        }

        static String LAMBDA_TRACE_HEADER_PROP = "com.amazonaws.xray.traceHeader";
    }

    static InvocationRequest invocationRequest;
}
