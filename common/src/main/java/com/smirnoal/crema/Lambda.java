package com.smirnoal.crema;

import com.smirnoal.crema.log.LambdaLogger;

import java.util.ArrayList;
import java.util.List;

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

        public static final String HANDLER = System.getenv("_HANDLER");
        public static final String LAMBDA_TELEMETRY_LOG_FD = System.getenv("_LAMBDA_TELEMETRY_LOG_FD");
        public static final String AWS_DEFAULT_REGION = System.getenv("AWS_DEFAULT_REGION");
        public static final String AWS_REGION = System.getenv("AWS_REGION");
        public static final String AWS_EXECUTION_ENV = System.getenv("AWS_EXECUTION_ENV");
        public static final String AWS_LAMBDA_FUNCTION_NAME = System.getenv("AWS_LAMBDA_FUNCTION_NAME");
        public static final String AWS_LAMBDA_FUNCTION_MEMORY_SIZE = System.getenv("AWS_LAMBDA_FUNCTION_MEMORY_SIZE");
        public static final String AWS_LAMBDA_FUNCTION_VERSION = System.getenv("AWS_LAMBDA_FUNCTION_VERSION");
        public static final String AWS_LAMBDA_INITIALIZATION_TYPE = System.getenv("AWS_LAMBDA_INITIALIZATION_TYPE");
        public static final String AWS_LAMBDA_LOG_GROUP_NAME = System.getenv("AWS_LAMBDA_LOG_GROUP_NAME");
        public static final String AWS_LAMBDA_LOG_STREAM_NAME = System.getenv("AWS_LAMBDA_LOG_STREAM_NAME");
        public static final String AWS_LAMBDA_LOG_FORMAT = getEnvOrDefault("AWS_LAMBDA_LOG_FORMAT", "TEXT");
        public static final String AWS_LAMBDA_LOG_LEVEL = getEnvOrDefault("AWS_LAMBDA_LOG_LEVEL", "UNDEFINED");
        public static final String AWS_ACCESS_KEY = System.getenv("AWS_ACCESS_KEY");
        public static final String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
        public static final String AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
        public static final String AWS_SESSION_TOKEN = System.getenv("AWS_SESSION_TOKEN");
        public static final String AWS_LAMBDA_RUNTIME_API = System.getenv("AWS_LAMBDA_RUNTIME_API");
        public static final String LAMBDA_TASK_ROOT = System.getenv("LAMBDA_TASK_ROOT");
        public static final String LAMBDA_RUNTIME_DIR = System.getenv("LAMBDA_RUNTIME_DIR");
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String v = System.getenv(name);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }

    public static final class SnapStart {
        private static final List<Runnable> BEFORE_SNAPSHOT = new ArrayList<>();
        private static final List<Runnable> AFTER_RESTORE = new ArrayList<>();

        public static void registerBeforeSnapshotHook(Runnable runnable) {
            SnapStart.BEFORE_SNAPSHOT.add(runnable);
        }

        public static void registerAfterRestoreHook(Runnable runnable) {
            SnapStart.AFTER_RESTORE.add(runnable);
        }

        static List<Runnable> getBeforeSnapshot() {
            return SnapStart.BEFORE_SNAPSHOT;
        }

        static List<Runnable> getAfterRestore() {
            return SnapStart.AFTER_RESTORE;
        }
    }

    public static final class Constants {
        private Constants() {
        }

        public static final String LAMBDA_TRACE_HEADER_PROP = "com.amazonaws.xray.traceHeader";
        public static final String INITIALIZATION_TYPE_SNAP_START = "snap-start";
        public static final String ERROR_TYPE_BEFORE_SNAPSHOT = "Runtime.BeforeSnapshotError";
        public static final String ERROR_TYPE_AFTER_RESTORE = "Runtime.AfterRestoreError";
        public static final String AWS_LAMBDA_LOG_FORMAT_JSON = "JSON";
    }

    private static volatile InvocationContext currentContext;

    /**
     * Returns the invocation context for the current invocation, or null when
     * called outside a handler (e.g. during initialization).
     */
    public static InvocationContext context() {
        return currentContext;
    }

    static void setCurrentContext(InvocationContext context) {
        currentContext = context;
    }

    /**
     * Returns the Lambda logger. Use {@code info()}, {@code debug()}, etc. to
     * log with levels. Output format is text or JSON depending on
     * AWS_LAMBDA_LOG_FORMAT; AWS_LAMBDA_LOG_LEVEL controls minimum level when
     * configured via LoggingConfig.
     */
    public static LambdaLogger logger() {
        return LambdaLogger.instance();
    }
}
