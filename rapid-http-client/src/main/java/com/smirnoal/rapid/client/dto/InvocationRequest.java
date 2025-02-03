package com.smirnoal.rapid.client.dto;

public class InvocationRequest {
    public String clientContext;
    public String cognitoIdentity;
    public byte[] content;
    public long deadlineTimeInMs;
    public String id;
    public String invokedFunctionArn;
    public String xrayTraceId;
}
