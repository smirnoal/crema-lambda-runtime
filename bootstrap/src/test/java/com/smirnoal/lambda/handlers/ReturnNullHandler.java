package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaHandlerBuilder;

public class ReturnNullHandler {

    public String handle(String event) {
        return null;
    }

    public static void main(String[] args) {
        ReturnNullHandler myHandler = new ReturnNullHandler();

        LambdaHandler<String, String> handler = new LambdaHandlerBuilder<String, String>()
                .withInputTypeDeserializer(String::new)
                .withOutputTypeSerializer(String::getBytes)
                .withHandler(myHandler::handle)
                .build();

        LambdaApplication app = new LambdaApplication();
        app.run(handler);
    }
}
