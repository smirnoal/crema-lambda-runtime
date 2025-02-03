package com.smirnoal.lambda;

import com.smirnoal.rapid.client.dto.InvocationRequest;

public class EventContext {
    public final long deadlineTimeInMs;
    public final String awsRequestId;
    public final String invokedFunctionArn;

    public EventContext(InvocationRequest request) {
        this.deadlineTimeInMs = request.deadlineTimeInMs;
        this.awsRequestId = request.id;
        this.invokedFunctionArn = request.invokedFunctionArn;
    }
}
