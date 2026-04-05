package com.smirnoal.crema.log;

import com.smirnoal.crema.LambdaTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.smirnoal.crema.LambdaTestUtils.withFormattingSink;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormattingLogSinkTest {

    @Test
    void log_whenJsonFormat_producesStructuredOutput(@TempDir Path tempDir) throws Exception {
        try (AutoCloseable env = LambdaTestUtils.withEnvironment("AWS_LAMBDA_LOG_FORMAT", "JSON")) {
            Path tempFile = Files.createTempFile(tempDir, "format-sink-json", ".log");
            withFormattingSink(tempFile, sink ->
                    sink.log(LogLevel.INFO, "test message", null, null));
            byte[] output = Files.readAllBytes(tempFile);
            String message = new String(output, 16, output.length - 16, StandardCharsets.UTF_8);
            assertTrue(message.startsWith("{"));
            assertTrue(message.endsWith("}\n"));
            assertTrue(message.contains("\"timestamp\":"));
            assertTrue(message.contains("\"level\":\"INFO\""));
            assertTrue(message.contains("\"message\":\"test message\""));
        }
    }

    @Test
    void log_whenJsonFormatAndLevelInfo_filtersDebugButPassesInfo(@TempDir Path tempDir) throws Exception {
        try (AutoCloseable formatEnv = LambdaTestUtils.withEnvironment("AWS_LAMBDA_LOG_FORMAT", "JSON");
             AutoCloseable levelEnv = LambdaTestUtils.withEnvironment("AWS_LAMBDA_LOG_LEVEL", "INFO")) {
            Path tempFile = Files.createTempFile(tempDir, "format-sink-filter", ".log");
            withFormattingSink(tempFile, sink -> {
                sink.log(LogLevel.DEBUG, "should be filtered", null, null);
                sink.log(LogLevel.INFO, "should pass through", null, null);
            });
            byte[] output = Files.readAllBytes(tempFile);
            assertTrue(output.length > 16);
            String payload = new String(output, 16, output.length - 16, StandardCharsets.UTF_8);
            assertTrue(payload.contains("\"message\":\"should pass through\""), "INFO message should pass through");
            assertTrue(payload.contains("\"level\":\"INFO\""));
            assertFalse(payload.contains("should be filtered"), "DEBUG message should be filtered");
        }
    }

    @Test
    void log_passesThroughToDelegate(@TempDir Path tempDir) throws Exception {
        Path tempFile = Files.createTempFile(tempDir, "format-sink-test", ".log");
        withFormattingSink(tempFile, sink ->
                sink.log(LogLevel.INFO, "test message", null, null));
        byte[] output = Files.readAllBytes(tempFile);
        assertTrue(output.length > 16);
        String message = new String(output, 16, output.length - 16, StandardCharsets.UTF_8);
        assertTrue(message.contains("test message"));
    }

    @Test
    void log_whenStructuredFieldsSerializationFails_emitsLineWithoutExtraFields(@TempDir Path tempDir) throws Exception {
        Number bad = new Number() {
            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0;
            }

            @Override
            public float floatValue() {
                return 0;
            }

            @Override
            public double doubleValue() {
                return 0;
            }

            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        };
        StructuredFields fields = StructuredFields.of("k", bad);
        try (AutoCloseable env = LambdaTestUtils.withEnvironment("AWS_LAMBDA_LOG_FORMAT", "JSON")) {
            Path tempFile = Files.createTempFile(tempDir, "format-sink-fields-fail", ".log");
            withFormattingSink(tempFile, sink ->
                    sink.log(LogLevel.INFO, "hello", null, fields));
            byte[] output = Files.readAllBytes(tempFile);
            String message = new String(output, 16, output.length - 16, StandardCharsets.UTF_8);
            assertTrue(message.contains("\"message\":\"hello\""), message);
            assertFalse(message.contains("\"k\""), message);
        }
    }

    @Test
    void logRaw_passesBytesThrough(@TempDir Path tempDir) throws Exception {
        Path tempFile = Files.createTempFile(tempDir, "format-sink-raw", ".log");
        withFormattingSink(tempFile, sink ->
                sink.logRaw("raw bytes".getBytes(StandardCharsets.UTF_8)));
        byte[] output = Files.readAllBytes(tempFile);
        assertTrue(output.length > 16);
        String message = new String(output, 16, output.length - 16, StandardCharsets.UTF_8);
        assertEquals("raw bytes", message);
    }
}
