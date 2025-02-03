package com.smirnoal.rapid.client.dto;

public class ErrorRequest {
    public String errorMessage;
    public String errorType;
    public String[] stackTrace;

    @SuppressWarnings("unused")
    public ErrorRequest() {
    }

    public ErrorRequest(String errorMessage, String errorType, String[] stackTrace) {
        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.stackTrace = stackTrace;
    }
}
