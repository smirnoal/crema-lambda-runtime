package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;

public class ThrowsCycleHandler implements LambdaHandler {
    @Override
    public byte[] handle(byte[] event) {
        RuntimeException e1 = new RuntimeException("exception 1");
        RuntimeException e2 = new RuntimeException("exception 2", e1);
        e1.initCause(e2);
        throw e1;
    }

    public static void main(String[] args) {
        LambdaApplication app = new LambdaApplication(new ThrowsCycleHandler());
        app.run();
    }
}
