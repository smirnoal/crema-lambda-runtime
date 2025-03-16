package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.StringLambdaHandler;

public class ThrowsHandler extends StringLambdaHandler {
    @Override
    public String handle(String event) {
        throw new MySpecialException("exception message");
    }

    static class MySpecialException extends RuntimeException {
        MySpecialException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        LambdaApplication<String, String> app = new LambdaApplication<>(new ThrowsHandler());
        app.run();
    }
}
