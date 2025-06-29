package com.smirnoal.lambda;

/**
 * Configuration for a Lambda handler containing the handler class name and JAR path.
 * This is used for isolated testing of individual handlers.
 */
public record HandlerConfig(String handlerClassName, String jarPath) {
    
    /**
     * Creates a HandlerConfig with the specified handler class name and JAR path.
     * 
     * @param handlerClassName the fully qualified class name of the handler
     * @param jarPath the path to the handler's JAR file
     */
    public HandlerConfig {
        if (handlerClassName == null || handlerClassName.trim().isEmpty()) {
            throw new IllegalArgumentException("Handler class name cannot be null or empty");
        }
        if (jarPath == null || jarPath.trim().isEmpty()) {
            throw new IllegalArgumentException("JAR path cannot be null or empty");
        }
    }
} 