package com.smirnoal.lambda.handlers;

import com.smirnoal.lambda.StringLambdaHandler;

public class ReverseStringHandler extends StringLambdaHandler {
    @Override
    public String handle(String event) {
        return new StringBuilder(event)
                .reverse()
                .toString();
    }
}
