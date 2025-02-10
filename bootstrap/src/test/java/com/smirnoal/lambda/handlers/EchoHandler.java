package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaHandler;

public class EchoHandler implements LambdaHandler {
    @Override
    public byte[] handle(byte[] event) {
        return event;
    }
}
