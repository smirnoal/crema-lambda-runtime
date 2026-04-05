package com.smirnoal.crema.rapid.client.dto;

public record ErrorRequest(
        String errorMessage,
        String errorType,
        String[] stackTrace) {

    public static ErrorRequestBuilder builder() {
        return new ErrorRequestBuilder();
    }

    public static class ErrorRequestBuilder {
        String errorMessage;
        String errorType;
        String[] stackTrace;

        private ErrorRequestBuilder() {
        }

        public ErrorRequestBuilder withErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ErrorRequestBuilder withErrorType(String errorType) {
            this.errorType = errorType;
            return this;
        }

        public ErrorRequestBuilder withStackTrace(String[] stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public ErrorRequest build() {
            return new ErrorRequest(errorMessage, errorType, stackTrace);
        }
    }
}
