package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaHandler;
import com.smirnoal.lambda.LambdaApplication;

public class RunnableHandler {

    public void voidHandler() {
    }

    public static void main(String[] args) {
        RunnableHandler myHandler = new RunnableHandler();
        LambdaHandler<Void, Void> handler = new LambdaHandler<Void, Void>()
                .withHandler(myHandler::voidHandler);

        LambdaApplication app = new LambdaApplication();
        app.run(handler);
    }
}
