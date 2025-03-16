package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaHandlerBuilder;

public class EchoStringHandler {

    public String handle(String event) {
        return event;
    }

    public static void main(String[] args) {
        EchoStringHandler myHandler = new EchoStringHandler();

        LambdaHandler<String, String> handler = new LambdaHandlerBuilder<String, String>()
                .withInputTypeDeserializer(String::new)
                .withOutputTypeSerializer(String::getBytes)
                .withHandler(myHandler::handle)
                .build();

        LambdaApplication<String, String> app = new LambdaApplication<>(handler);
        app.run();
    }
}
