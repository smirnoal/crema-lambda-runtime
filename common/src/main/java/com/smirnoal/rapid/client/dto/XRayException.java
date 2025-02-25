package com.smirnoal.rapid.client.dto;

import java.util.List;

public class XRayException {
    public String message;
    public String type;
    public List<StackElement> stack;

    @SuppressWarnings("unused")
    public XRayException() {
    }

    public XRayException(String message, String type, List<StackElement> stack) {
        this.message = message;
        this.type = type;
        this.stack = stack;
    }

    @Override
    public String toString() {
        return "XRayException{" +
                "message='" + message + '\'' +
                ", type='" + type + '\'' +
                ", stack=" + stack +
                '}';
    }
}
