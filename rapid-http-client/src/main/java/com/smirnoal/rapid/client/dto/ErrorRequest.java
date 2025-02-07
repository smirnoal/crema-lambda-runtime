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

    public ErrorRequest withErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public ErrorRequest withErrorType(String errorType) {
        this.errorType = errorType;
        return this;
    }

    public ErrorRequest withStackTrace(String[] stackTrace) {
        this.stackTrace = stackTrace;
        return this;
    }
}
