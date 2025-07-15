package com.smirnoal.lambda;

public class HandlerConfig {
    private final String handlerClassName;
    private final String jarPath;

    public HandlerConfig(String handlerClassName, String handlerProject) {
        this.handlerClassName = handlerClassName;
        this.jarPath = String.format("build/handlers/%s/%s-1.0-SNAPSHOT.jar", handlerProject, handlerProject);
    }

    public String handlerClassName() { return handlerClassName; }
    public String jarPath() { return jarPath; }
} 