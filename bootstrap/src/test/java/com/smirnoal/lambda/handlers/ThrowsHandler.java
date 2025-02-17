package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;

public class ThrowsHandler implements LambdaHandler {
    @Override
    public byte[] handle(byte[] event) {
        throw new MySpecialException("exception message");
    }

    static class MySpecialException extends RuntimeException {
        MySpecialException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        LambdaApplication app = new LambdaApplication(new ThrowsHandler());
        app.run();
    }
}
