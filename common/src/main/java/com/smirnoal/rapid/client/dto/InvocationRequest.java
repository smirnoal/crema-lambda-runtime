package com.smirnoal.rapid.client.dto;

public class InvocationRequest {
    public final String clientContext;
    public final String cognitoIdentity;
    public final byte[] content;
    public final long deadlineTimeInMs;
    public final String id;
    public final String invokedFunctionArn;
    public final String xrayTraceId;

    public InvocationRequest(String clientContext, String cognitoIdentity, byte[] content, long deadlineTimeInMs, String id, String invokedFunctionArn, String xrayTraceId) {
        this.clientContext = clientContext;
        this.cognitoIdentity = cognitoIdentity;
        this.content = content;
        this.deadlineTimeInMs = deadlineTimeInMs;
        this.id = id;
        this.invokedFunctionArn = invokedFunctionArn;
        this.xrayTraceId = xrayTraceId;
    }

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

        InvocationRequestBuilder() {
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
            return new InvocationRequest(clientContext, cognitoIdentity, content, deadlineTimeInMs, id, invokedFunctionArn, xrayTraceId);
        }
    }
}
