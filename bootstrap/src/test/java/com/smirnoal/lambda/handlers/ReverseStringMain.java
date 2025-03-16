package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaHandlerBuilder;
import com.smirnoal.lambda.serde.StringSerDe;

public class ReverseStringMain {
    public static void main(String[] args) {
        LambdaApplication app = new LambdaApplication();

        ReverseStringHandler myObj = new ReverseStringHandler();
        LambdaHandler<String, String> handler = new LambdaHandlerBuilder<String, String>()
                .withHandler(myObj::handle)
                .withLambdaSerde(new StringSerDe())
                .build();
        app.run(handler);
    }
}
