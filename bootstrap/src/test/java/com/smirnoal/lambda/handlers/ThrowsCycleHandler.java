package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.StringLambdaHandler;

public class ThrowsCycleHandler extends StringLambdaHandler {
    @Override
    public String handle(String event) {
        RuntimeException e1 = new RuntimeException("exception 1");
        RuntimeException e2 = new RuntimeException("exception 2", e1);
        e1.initCause(e2);
        throw e1;
    }

    public static void main(String[] args) {
        LambdaApplication<String, String> app = new LambdaApplication<>(new ThrowsCycleHandler());
        app.run();
    }
}
