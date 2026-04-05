package com.smirnoal.crema.log;

import com.smirnoal.crema.InvocationContext;

import java.io.IOException;
import com.smirnoal.crema.Lambda;
import com.smirnoal.crema.rapid.client.dto.InvocationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LambdaLogFormatterTest {

    @AfterEach
    void tearDown() throws Exception {
        var setContext = Lambda.class.getDeclaredMethod("setCurrentContext", InvocationContext.class);
        setContext.setAccessible(true);
        setContext.invoke(null, new Object[]{null});
    }

    @Test
    void formatText_undefinedLevel_returnsTimestampRequestIdLevelMessage() {
        LambdaLogFormatter formatter = new LambdaLogFormatter(false);
        String result = formatter.format(LogLevel.UNDEFINED, "hello");
        // Format: timestamp\trequestId\tLEVEL\tmessage (matches Node.js/Python)
        assertTrue(result.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}Z\t.*\tUNDEFINED\thello\n"), result);
    }

    @Test
    void formatText_infoLevel_returnsTimestampRequestIdLevelMessage() {
        LambdaLogFormatter formatter = new LambdaLogFormatter(false);
        String result = formatter.format(LogLevel.INFO, "hello");
        // Format: timestamp\trequestId\tLEVEL\tmessage (matches Node.js/Python)
        assertTrue(result.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}Z\t.*\tINFO\thello\n"), result);
    }

    @Test
    void formatJson_producesValidJson() {
        LambdaLogFormatter formatter = new LambdaLogFormatter(true);
        String result = formatter.format(LogLevel.INFO, "test message");
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}\n"));
        assertTrue(result.contains("\"timestamp\":"));
        assertTrue(result.contains("\"level\":\"INFO\""));
        assertTrue(result.contains("\"message\":\"test message\""));
    }

    @Test
    void formatJson_withContext_includesRequestId() throws Exception {
        var request = InvocationRequest.builder()
                .withId("req-123")
                .withDeadlineTimeInMs(System.currentTimeMillis() + 60_000)
                .withInvokedFunctionArn("arn:aws:lambda:us-east-1:123:function:test")
                .build();
        var setContext = Lambda.class.getDeclaredMethod("setCurrentContext", InvocationContext.class);
        setContext.setAccessible(true);
        setContext.invoke(null, new InvocationContext(request));

        LambdaLogFormatter formatter = new LambdaLogFormatter(true);
        String result = formatter.format(LogLevel.INFO, "msg");

        assertTrue(result.contains("\"requestId\":\"req-123\""));
    }

    @Test
    void formatJson_escapesSpecialChars() {
        LambdaLogFormatter formatter = new LambdaLogFormatter(true);
        String result = formatter.format(LogLevel.INFO, "say \"hello\"");
        assertTrue(result.contains("\\\"hello\\\""));
    }

    @Test
    void formatJson_nullMessage_usesNullString() {
        LambdaLogFormatter formatter = new LambdaLogFormatter(true);
        String result = formatter.format(LogLevel.INFO, null);
        assertTrue(result.contains("\"message\":\"null\""));
    }

    @Test
    void formatJson_withThrowable_includesErrorFields() {
        LambdaLogFormatter formatter = new LambdaLogFormatter(true);
        IllegalArgumentException ex = new IllegalArgumentException("invalid input");
        String result = formatter.format(LogLevel.ERROR, "Operation failed", ex);

        assertTrue(result.contains("\"errorType\":\"IllegalArgumentException\""));
        assertTrue(result.contains("\"errorMessage\":\"invalid input\""));
        assertTrue(result.contains("\"stackTrace\":["));
        assertTrue(result.contains("java.lang.IllegalArgumentException"));
    }

    @Test
    void formatJson_withThrowableCause_includesCauseInStackTrace() {
        LambdaLogFormatter formatter = new LambdaLogFormatter(true);
        IOException root = new IOException("root cause");
        RuntimeException ex = new RuntimeException("wrapped", root);
        String result = formatter.format(LogLevel.ERROR, "Failed", ex);

        assertTrue(result.contains("\"errorType\":\"RuntimeException\""));
        assertTrue(result.contains("\"errorMessage\":\"wrapped\""));
        assertTrue(result.contains("\"Caused by: java.io.IOException: root cause\""), result);
    }

    @Test
    void formatJson_withThrowableSuppressed_includesSuppressedInStackTrace() {
        LambdaLogFormatter formatter = new LambdaLogFormatter(true);
        Exception suppressed = new Exception("suppressed msg");
        Exception ex = new Exception("primary");
        ex.addSuppressed(suppressed);
        String result = formatter.format(LogLevel.ERROR, "Failed", ex);

        assertTrue(result.contains("\"Suppressed: java.lang.Exception: suppressed msg\""), result);
    }

    @Test
    void formatText_withThrowableCause_includesCauseAndStackTrace() {
        LambdaLogFormatter formatter = new LambdaLogFormatter(false);
        IOException root = new IOException("root cause");
        RuntimeException ex = new RuntimeException("wrapped", root);
        String result = formatter.format(LogLevel.ERROR, "Failed", ex);

        assertTrue(result.contains("Caused by: java.io.IOException: root cause"), result);
        assertTrue(result.contains("\tat "), result);
    }

    @Test
    void formatJson_structuredFieldToStringThrows_wrapsStructuredFieldsSerializationException() {
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
                throw new IllegalStateException("bad");
            }
        };
        StructuredFields fields = StructuredFields.of("k", bad);
        LambdaLogFormatter formatter = new LambdaLogFormatter(true);
        var ex = assertThrows(StructuredFieldsSerializationException.class,
                () -> formatter.format(LogLevel.INFO, "msg", null, fields));
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    @Test
    void formatText_structuredFieldToStringThrows_wrapsStructuredFieldsSerializationException() {
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
                throw new IllegalStateException("bad");
            }
        };
        StructuredFields fields = StructuredFields.of("k", bad);
        LambdaLogFormatter formatter = new LambdaLogFormatter(false);
        var ex = assertThrows(StructuredFieldsSerializationException.class,
                () -> formatter.format(LogLevel.INFO, "msg", null, fields));
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    @Test
    void formatJson_structuredFields_nonFiniteDoubles_emitNull() {
        LambdaLogFormatter formatter = new LambdaLogFormatter(true);
        StructuredFields fields = StructuredFields.builder()
                .put("posInf", Double.POSITIVE_INFINITY)
                .put("nan", Double.NaN)
                .build();
        String result = formatter.format(LogLevel.INFO, "msg", null, fields);

        assertTrue(result.contains("\"posInf\":null"), result);
        assertTrue(result.contains("\"nan\":null"), result);
        assertFalse(result.contains("Infinity"), result);
        assertFalse(result.contains("NaN"), result);
    }
}
