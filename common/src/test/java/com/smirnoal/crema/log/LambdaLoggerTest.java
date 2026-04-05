package com.smirnoal.crema.log;

import com.smirnoal.crema.Lambda;
import com.smirnoal.crema.json.MinimalJsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LambdaLoggerTest {

    private ByteArrayOutputStream capture;

    @AfterEach
    void tearDown() {
        LambdaLogSinkHolder.set(null);
        if (capture != null) {
            System.setErr(new PrintStream(capture));
        }
    }

    @Test
    void logger_returnsSameInstance() {
        assertSame(Lambda.logger(), Lambda.logger());
    }

    @Test
    void info_whenSinkNotSet_writesToStderr() {
        capture = new ByteArrayOutputStream();
        System.setErr(new PrintStream(capture));

        Lambda.logger().info("fallback message");

        assertTrue(capture.toString().contains("fallback message"));
    }

    @Test
    void info_whenSinkSet_routesToSink() {
        var captured = new StringBuilder();
        LambdaLogSinkHolder.set((level, message, throwable, fields) ->
                captured.append(level).append(":").append(message));

        Lambda.logger().info("routed message");

        assertEquals("INFO:routed message", captured.toString());
    }

    @Test
    void error_withThrowable_routesToSink() {
        var captured = new StringBuilder();
        Throwable[] received = new Throwable[1];
        LambdaLogSinkHolder.set((level, message, throwable, fields) -> {
            captured.append(level).append(":").append(message);
            received[0] = throwable;
        });

        RuntimeException ex = new RuntimeException("test error");
        Lambda.logger().error("Operation failed", ex);

        assertEquals("ERROR:Operation failed", captured.toString());
        assertSame(ex, received[0]);
    }

    @Test
    void debug_withSupplier_whenSinkNotSet_invokesSupplier() {
        capture = new ByteArrayOutputStream();
        System.setErr(new PrintStream(capture));
        var invoked = new AtomicInteger(0);

        Lambda.logger().debug(() -> {
            invoked.incrementAndGet();
            return "lazy message";
        });

        assertEquals(1, invoked.get());
        assertTrue(capture.toString().contains("lazy message"));
    }

    @Test
    void isInfoEnabled_whenSinkNotSet_returnsTrue() {
        assertTrue(Lambda.logger().isInfoEnabled());
    }

    @Test
    void parsesRealEmfMetricAndMergesIntoLogOutput() {
        String emfJson = """
            {
                "_aws": {
                    "Timestamp": 1711272000000,
                    "CloudWatchMetrics": [
                        {
                            "Namespace": "MyService/API",
                            "Dimensions": [
                                ["Operation"],
                                ["Operation", "Cell"]
                            ],
                            "Metrics": [
                                {"Name": "ProcessingLatency", "Unit": "Milliseconds", "StorageResolution": 60},
                                {"Name": "RequestCount", "Unit": "Count"},
                                {"Name": "ErrorCount", "Unit": "Count"}
                            ]
                        }
                    ]
                },
                "Operation": "GetItem",
                "Cell": "us-east-1-cell-3",
                "ProcessingLatency": 142,
                "RequestCount": 1,
                "ErrorCount": 0
            }
            """.stripIndent();

        Map<String, Object> parsed = MinimalJsonParser.parseObject(emfJson);

        assertEquals("GetItem", parsed.get("Operation"));
        assertEquals("us-east-1-cell-3", parsed.get("Cell"));
        assertEquals(142, parsed.get("ProcessingLatency"));
        assertEquals(1, parsed.get("RequestCount"));
        assertEquals(0, parsed.get("ErrorCount"));

        @SuppressWarnings("unchecked")
        Map<String, Object> aws = (Map<String, Object>) parsed.get("_aws");
        assertEquals(1711272000000L, aws.get("Timestamp"));

        @SuppressWarnings("unchecked")
        List<Object> cwMetrics = (List<Object>) aws.get("CloudWatchMetrics");
        assertEquals(1, cwMetrics.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> directive = (Map<String, Object>) cwMetrics.get(0);
        assertEquals("MyService/API", directive.get("Namespace"));

        @SuppressWarnings("unchecked")
        List<Object> metrics = (List<Object>) directive.get("Metrics");
        assertEquals(3, metrics.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> latencyMetric = (Map<String, Object>) metrics.get(0);
        assertEquals("ProcessingLatency", latencyMetric.get("Name"));
        assertEquals("Milliseconds", latencyMetric.get("Unit"));
        assertEquals(60, latencyMetric.get("StorageResolution"));

        StructuredFields fields = StructuredFields.builder()
                .mergeRawJson(emfJson)
                .put("traceId", "1-67e1a2b3-abcdef0123456789abcdef01")
                .build();

        String logOutput = captureLogOutput(() -> Lambda.logger().info("GetItem completed", fields));

        assertFalse(logOutput.contains("\\\"_aws\\\""),
                "EMF payload must not be double-escaped");
        assertTrue(logOutput.contains("\"_aws\":{\"Timestamp\":1711272000000"));
        assertTrue(logOutput.contains("\"ProcessingLatency\":142"));
        assertTrue(logOutput.contains("\"message\":\"GetItem completed\""));
        assertTrue(logOutput.contains("\"traceId\":\"1-67e1a2b3-abcdef0123456789abcdef01\""));
    }

    private static String captureLogOutput(Runnable action) {
        LambdaLogSink previous = LambdaLogSinkHolder.get();
        LambdaLogFormatter formatter = new LambdaLogFormatter(true);
        StringBuilder captured = new StringBuilder();
        try {
            LambdaLogSinkHolder.set((level, message, throwable, fields) ->
                    captured.append(formatter.format(level, message, throwable, fields)));
            action.run();
            return captured.toString();
        } finally {
            LambdaLogSinkHolder.set(previous);
        }
    }
}
