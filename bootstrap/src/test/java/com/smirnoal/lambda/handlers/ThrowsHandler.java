package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.serde.StringSerde;

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
        ThrowsHandler myObject = new ThrowsHandler();
        LambdaHandler<String, String> handler = new LambdaHandler<String, String>()
                .withHandler(myObject::handle)
                .withLambdaSerde(new StringSerde());

        LambdaApplication app = new LambdaApplication();
        app.run(handler);
    }
}
