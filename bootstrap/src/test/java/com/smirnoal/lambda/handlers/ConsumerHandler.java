package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaHandlerBuilder;

public class ConsumerHandler {

    public void voidHandler(String event) {
    }

    public static void main(String[] args) {
        ConsumerHandler myHandler = new ConsumerHandler();

        LambdaHandler<String, Void> handler = new LambdaHandlerBuilder<String, Void>()
                .withInputTypeDeserializer(String::new)
                .withHandler(myHandler::voidHandler)
                .build();

        LambdaApplication app = new LambdaApplication();
        app.run(handler);
    }
}
