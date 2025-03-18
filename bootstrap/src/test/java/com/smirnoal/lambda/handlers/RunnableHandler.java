package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaApplication;
import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaHandlerBuilder;

public class RunnableHandler {

    public void voidHandler() {
    }

    public static void main(String[] args) {
        RunnableHandler myHandler = new RunnableHandler();
        LambdaHandler<Void, Void> handler = new LambdaHandlerBuilder<Void, Void>()
                .withHandler(myHandler::voidHandler)
                .build();

        LambdaApplication app = new LambdaApplication();
        app.run(handler);
    }
}
