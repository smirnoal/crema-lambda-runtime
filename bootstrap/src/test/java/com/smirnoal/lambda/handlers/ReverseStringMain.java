package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;

public class ReverseStringMain {
    public static void main(String[] args) {
        LambdaApplication<String, String> app = new LambdaApplication<>(new ReverseStringHandler());
        app.run();
    }
}
