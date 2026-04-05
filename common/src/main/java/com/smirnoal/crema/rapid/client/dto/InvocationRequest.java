package com.smirnoal.crema.rapid.client.dto;

public record InvocationRequest(
        String id,
        byte[] content,
        String clientContext,
        String cognitoIdentity,
        long deadlineTimeInMs,
        String invokedFunctionArn,
        String xrayTraceId) {

    public static InvocationRequestBuilder builder() {
        return new InvocationRequestBuilder();
    }

    public static class InvocationRequestBuilder {
        String clientContext;
        String cognitoIdentity;
        byte[] content;
        long deadlineTimeInMs;
        String id;
        String invokedFunctionArn;
        String xrayTraceId;

        private InvocationRequestBuilder() {
        }

        public InvocationRequestBuilder withClientContext(String clientContext) {
            this.clientContext = clientContext;
            return this;
        }

        public InvocationRequestBuilder withCognitoIdentity(String cognitoIdentity) {
            this.cognitoIdentity = cognitoIdentity;
            return this;
        }

        public InvocationRequestBuilder withContent(byte[] content) {
            this.content = content;
            return this;
        }

        public InvocationRequestBuilder withDeadlineTimeInMs(long deadlineTimeInMs) {
            this.deadlineTimeInMs = deadlineTimeInMs;
            return this;
        }

        public InvocationRequestBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public InvocationRequestBuilder withInvokedFunctionArn(String invokedFunctionArn) {
            this.invokedFunctionArn = invokedFunctionArn;
            return this;
        }

        public InvocationRequestBuilder withXrayTraceId(String xrayTraceId) {
            this.xrayTraceId = xrayTraceId;
            return this;
        }

        public InvocationRequest build() {
            return new InvocationRequest(id, content, clientContext, cognitoIdentity, deadlineTimeInMs, invokedFunctionArn, xrayTraceId);
        }
    }
}
