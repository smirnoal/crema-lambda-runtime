package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.serde.StringSerde;

public class ReverseStringMain {
    public static void main(String[] args) {
        ReverseStringHandler myObj = new ReverseStringHandler();

        LambdaHandler<String, String> handler = new LambdaHandler<String, String>()
                .withHandler(myObj::handle)
                .withLambdaSerde(new StringSerde());

        LambdaApplication app = new LambdaApplication();
        app.run(handler);
    }
}
