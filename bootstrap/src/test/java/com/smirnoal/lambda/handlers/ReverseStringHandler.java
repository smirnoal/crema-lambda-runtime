package com.smirnoal.lambda.handlers;

public class ReverseStringHandler {
    public String handle(String event) {
        return new StringBuilder(event)
                .reverse()
                .toString();
    }
}
