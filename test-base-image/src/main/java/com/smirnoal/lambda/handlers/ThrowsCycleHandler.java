package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaApplication;

public class ThrowsCycleHandler {

    public void handle() {
        RuntimeException e1 = new RuntimeException("exception 1");
        RuntimeException e2 = new RuntimeException("exception 2", e1);
        e1.initCause(e2);
        throw e1;
    }

    public static void main(String[] args) {
        ThrowsCycleHandler myObject = new ThrowsCycleHandler();
        LambdaHandler<Void, Void> handler = new LambdaHandler<Void, Void>()
                .withHandler(myObject::handle);

        LambdaApplication app = new LambdaApplication();
        app.run(handler);
    }
}
