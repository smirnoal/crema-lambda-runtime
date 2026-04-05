package com.smirnoal.crema.rapid.client.serde;

import com.smirnoal.crema.json.JsonText;
import com.smirnoal.crema.rapid.client.dto.ErrorRequest;
import com.smirnoal.crema.rapid.client.dto.StackElement;
import com.smirnoal.crema.rapid.client.dto.XRayErrorCause;
import com.smirnoal.crema.rapid.client.dto.XRayException;

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
            json.append("\"errorMessage\":");
            JsonText.appendJsonString(errorRequest.errorMessage(), json);
        }
        
        if (errorRequest.errorType() != null) {
            if (json.length() > 1) json.append(",");
            json.append("\"errorType\":");
            JsonText.appendJsonString(errorRequest.errorType(), json);
        }
        
        if (errorRequest.stackTrace() != null) {
            if (json.length() > 1) json.append(",");
            json.append("\"stackTrace\":[");
            String[] stackTrace = errorRequest.stackTrace();
            for (int i = 0; i < stackTrace.length; i++) {
                if (i > 0) json.append(",");
                JsonText.appendJsonString(stackTrace[i], json);
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
            json.append("\"working_directory\":");
            JsonText.appendJsonString(xRayErrorCause.workingDirectory(), json);
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
                JsonText.appendJsonString(path, json);
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
            json.append("\"message\":");
            JsonText.appendJsonString(exception.message(), json);
        }
        
        if (exception.type() != null) {
            if (json.length() > 1) json.append(",");
            json.append("\"type\":");
            JsonText.appendJsonString(exception.type(), json);
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
            json.append("\"label\":");
            JsonText.appendJsonString(element.label(), json);
        }
        
        if (element.path() != null) {
            if (json.length() > 1) json.append(",");
            json.append("\"path\":");
            JsonText.appendJsonString(element.path(), json);
        }
        
        if (json.length() > 1) json.append(",");
        json.append("\"line\":").append(element.line());
        
        json.append("}");
        return json.toString();
    }
    
} 