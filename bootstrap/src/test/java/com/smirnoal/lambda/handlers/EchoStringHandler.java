package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;

public class EchoStringHandler {

    public String handle(String event) {
        return event;
    }

    public static void main(String[] args) {
        EchoStringHandler myHandler = new EchoStringHandler();

        LambdaHandler<String, String> handler = new LambdaHandler<String, String>()
                .withInputTypeDeserializer(String::new)
                .withOutputTypeSerializer(String::getBytes)
                .withHandler(myHandler::handle);

        LambdaApplication app = new LambdaApplication();
        app.run(handler);
    }
}
