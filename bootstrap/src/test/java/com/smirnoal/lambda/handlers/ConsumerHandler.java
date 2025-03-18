package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaApplication;

public class ConsumerHandler {

    public void voidHandler(String event) {
    }

    public static void main(String[] args) {
        ConsumerHandler myHandler = new ConsumerHandler();

        LambdaHandler<String, Void> handler = new LambdaHandler<String, Void>()
                .withInputTypeDeserializer(String::new)
                .withHandler(myHandler::voidHandler);

        LambdaApplication app = new LambdaApplication();
        app.run(handler);
    }
}
