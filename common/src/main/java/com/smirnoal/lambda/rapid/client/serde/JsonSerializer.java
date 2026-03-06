package com.smirnoal.lambda.rapid.client.serde;

import com.smirnoal.lambda.rapid.client.dto.ErrorRequest;
import com.smirnoal.lambda.serde.JsonEscape;
import com.smirnoal.lambda.rapid.client.dto.StackElement;
import com.smirnoal.lambda.rapid.client.dto.XRayErrorCause;
import com.smirnoal.lambda.rapid.client.dto.XRayException;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * Custom JSON serializer for Lambda Rapid HTTP Client DTOs.
 * Provides proper string escaping for valid JSON output.
 */
public class JsonSerializer {
    
    /**
     * Serialize ErrorRequest to JSON bytes
     */
    public static byte[] serialize(ErrorRequest errorRequest) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        if (errorRequest.errorMessage() != null) {
            json.append("\"errorMessage\":").append(JsonEscape.escape(errorRequest.errorMessage()));
        }
        
        if (errorRequest.errorType() != null) {
            if (json.length() > 1) json.append(",");
            json.append("\"errorType\":").append(JsonEscape.escape(errorRequest.errorType()));
        }
        
        if (errorRequest.stackTrace() != null) {
            if (json.length() > 1) json.append(",");
            json.append("\"stackTrace\":[");
            String[] stackTrace = errorRequest.stackTrace();
            for (int i = 0; i < stackTrace.length; i++) {
                if (i > 0) json.append(",");
                json.append(JsonEscape.escape(stackTrace[i]));
            }
            json.append("]");
        }
        
        json.append("}");
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Serialize XRayErrorCause to JSON bytes
     */
    public static byte[] serialize(XRayErrorCause xRayErrorCause) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        if (xRayErrorCause.workingDirectory() != null) {
            json.append("\"working_directory\":").append(JsonEscape.escape(xRayErrorCause.workingDirectory()));
        }
        
        if (xRayErrorCause.exceptions() != null) {
            if (json.length() > 1) json.append(",");
            json.append("\"exceptions\":[");
            Collection<XRayException> exceptions = xRayErrorCause.exceptions();
            boolean first = true;
            for (XRayException exception : exceptions) {
                if (!first) json.append(",");
                json.append(serializeXRayException(exception));
                first = false;
            }
            json.append("]");
        }
        
        if (xRayErrorCause.paths() != null) {
            if (json.length() > 1) json.append(",");
            json.append("\"paths\":[");
            Collection<String> paths = xRayErrorCause.paths();
            boolean first = true;
            for (String path : paths) {
                if (!first) json.append(",");
                json.append(JsonEscape.escape(path));
                first = false;
            }
            json.append("]");
        }
        
        json.append("}");
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Serialize XRayException to JSON string (for use within arrays)
     */
    private static String serializeXRayException(XRayException exception) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        if (exception.message() != null) {
            json.append("\"message\":").append(JsonEscape.escape(exception.message()));
        }
        
        if (exception.type() != null) {
            if (json.length() > 1) json.append(",");
            json.append("\"type\":").append(JsonEscape.escape(exception.type()));
        }
        
        if (exception.stack() != null) {
            if (json.length() > 1) json.append(",");
            json.append("\"stack\":[");
            List<StackElement> stack = exception.stack();
            boolean first = true;
            for (StackElement element : stack) {
                if (!first) json.append(",");
                json.append(serializeStackElement(element));
                first = false;
            }
            json.append("]");
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Serialize StackElement to JSON string (for use within arrays)
     */
    private static String serializeStackElement(StackElement element) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        if (element.label() != null) {
            json.append("\"label\":").append(JsonEscape.escape(element.label()));
        }
        
        if (element.path() != null) {
            if (json.length() > 1) json.append(",");
            json.append("\"path\":").append(JsonEscape.escape(element.path()));
        }
        
        if (json.length() > 1) json.append(",");
        json.append("\"line\":").append(element.line());
        
        json.append("}");
        return json.toString();
    }
    
} 