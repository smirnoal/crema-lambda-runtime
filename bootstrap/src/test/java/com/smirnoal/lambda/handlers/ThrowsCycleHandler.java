package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaHandlerBuilder;
import com.smirnoal.lambda.serde.StringSerDe;

public class ThrowsCycleHandler {
    public String handle(String o) {
        RuntimeException e1 = new RuntimeException("exception 1");
        RuntimeException e2 = new RuntimeException("exception 2", e1);
        e1.initCause(e2);
        throw e1;
    }

    public static void main(String[] args) {
        LambdaApplication app = new LambdaApplication();

        ThrowsCycleHandler myObject = new ThrowsCycleHandler();
        LambdaHandler<String, String> handler = new LambdaHandlerBuilder<String, String>()
                .withHandler(myObject::handle)
                .withLambdaSerde(new StringSerDe())
                .build();
        app.run(handler);
    }
}
