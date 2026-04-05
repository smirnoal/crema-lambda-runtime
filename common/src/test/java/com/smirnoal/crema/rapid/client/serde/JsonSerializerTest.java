package com.smirnoal.crema.rapid.client.serde;

import com.smirnoal.crema.rapid.client.dto.ErrorRequest;
import com.smirnoal.crema.rapid.client.dto.StackElement;
import com.smirnoal.crema.rapid.client.dto.XRayErrorCause;
import com.smirnoal.crema.rapid.client.dto.XRayException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonSerializerTest {

    @Test
    void serializeErrorRequest_withSpecialCharacters() {
        ErrorRequest errorRequest = ErrorRequest.builder()
                .withErrorMessage("Error with \"quotes\" and \\backslashes\\ and \nnewlines\tand tabs")
                .withErrorType("TestError")
                .withStackTrace(new String[]{"stack trace line 1", "stack trace line 2"})
                .build();

        byte[] result = JsonSerializer.serialize(errorRequest);
        String json = new String(result);
        
        // Verify it's valid JSON by checking it contains properly escaped strings
        assertTrue(json.contains("\"errorMessage\":\"Error with \\\"quotes\\\" and \\\\backslashes\\\\ and \\nnewlines\\tand tabs\""));
        assertTrue(json.contains("\"errorType\":\"TestError\""));
        assertTrue(json.contains("\"stackTrace\":[\"stack trace line 1\",\"stack trace line 2\"]"));
        
        // Verify the JSON is well-formed
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
        assertTrue(json.contains("errorMessage"));
        assertTrue(json.contains("errorType"));
        assertTrue(json.contains("stackTrace"));
    }

    @Test
    void serializeXRayErrorCause_withSpecialCharacters() {
        XRayException exception = new XRayException(
                "Exception with \"quotes\" and \\backslashes\\",
                "TestException",
                List.of(StackElement.builder()
                        .withLabel("test label")
                        .withPath("/path/with/\"quotes\"")
                        .withLine(42)
                        .build())
        );

        XRayErrorCause xRayErrorCause = XRayErrorCause.builder()
                .withWorkingDirectory("/var/task")
                .withExceptions(List.of(exception))
                .withPaths(List.of("path1", "path2"))
                .build();

        byte[] result = JsonSerializer.serialize(xRayErrorCause);
        String json = new String(result);
        
        // Verify it's valid JSON
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
        assertTrue(json.contains("\"working_directory\":\"/var/task\""));
        assertTrue(json.contains("\"exceptions\":"));
        assertTrue(json.contains("\"paths\":"));
        
        // Verify the exception message is properly escaped
        assertTrue(json.contains("\"message\":\"Exception with \\\"quotes\\\" and \\\\backslashes\\\\\""));
        
        // Verify the stack element path is properly escaped
        assertTrue(json.contains("\"path\":\"/path/with/\\\"quotes\\\"\""));
    }

    @Test
    void serializeErrorRequest_nullValues() {
        ErrorRequest errorRequest = ErrorRequest.builder()
                .withErrorMessage("test message")
                .build();

        byte[] result = JsonSerializer.serialize(errorRequest);
        String json = new String(result);
        
        // Should only contain the non-null field
        assertTrue(json.contains("\"errorMessage\":\"test message\""));
        assertFalse(json.contains("errorType"));
        assertFalse(json.contains("stackTrace"));
    }

    @Test
    void serializeErrorRequest_emptyStackTrace() {
        ErrorRequest errorRequest = ErrorRequest.builder()
                .withErrorMessage("test message")
                .withErrorType("TestError")
                .withStackTrace(new String[0])
                .build();

        byte[] result = JsonSerializer.serialize(errorRequest);
        String json = new String(result);
        
        assertTrue(json.contains("\"stackTrace\":[]"));
    }

    @Test
    void serializeErrorRequest_withUnicodeCharacters() {
        String unicodeString = "emoji: 😃, cyrillic: Ж, chinese: 汉字, arabic: س, combining: e\u0301";
        ErrorRequest errorRequest = ErrorRequest.builder()
                .withErrorMessage(unicodeString)
                .withErrorType("UnicodeTest")
                .withStackTrace(new String[]{unicodeString})
                .build();

        byte[] result = JsonSerializer.serialize(errorRequest);
        String json = new String(result);

        // Check that the unicode characters are escaped as \\uXXXX
        assertTrue(json.contains("emoji: \\ud83d\\ude03, cyrillic: \\u0416, chinese: \\u6c49\\u5b57, arabic: \\u0633, combining: e\\u0301"));
        // The stack trace should also contain the escaped unicode string
        assertTrue(json.contains("\"stackTrace\":[\"emoji: \\ud83d\\ude03, cyrillic: \\u0416, chinese: \\u6c49\\u5b57, arabic: \\u0633, combining: e\\u0301\"]"));
    }
} 