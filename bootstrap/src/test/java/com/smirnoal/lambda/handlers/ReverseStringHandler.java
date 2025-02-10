package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.LambdaHandler;

public class ReverseStringHandler implements LambdaHandler {
    @Override
    public byte[] handle(byte[] event) {
        return new StringBuilder(new String(event))
                .reverse()
                .toString()
                .getBytes();
    }
}
