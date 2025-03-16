package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaHandlerBuilder;
import com.smirnoal.lambda.serde.StringSerDe;

public class ThrowsHandler {
    public String handle(String event) {
        throw new MySpecialException("exception message");
    }

    static class MySpecialException extends RuntimeException {
        MySpecialException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        LambdaApplication app = new LambdaApplication();
        ThrowsHandler myObject = new ThrowsHandler();
        LambdaHandler<String, String> handler = new LambdaHandlerBuilder<String, String>()
                .withHandler(myObject::handle)
                .withLambdaSerde(new StringSerDe())
                .build();
        app.run(handler);
    }
}
