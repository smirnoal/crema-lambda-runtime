package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;

public class EchoMain {
    public static void main(String[] args) {
        LambdaApplication app = new LambdaApplication(new EchoHandler());
        app.run();
    }
}
