package com.smirnoal.crema.rapid.client.dto;

import java.util.List;

public record XRayException(
        String message,
        String type,
        List<StackElement> stack) {

    @SuppressWarnings("unused")
    public XRayExceptionBuilder builder() {
        return new XRayExceptionBuilder();
    }

    public static class XRayExceptionBuilder {
        public String message;
        public String type;
        public List<StackElement> stack;

        private XRayExceptionBuilder() {
        }

        XRayExceptionBuilder withMessage(String message) {
            this.message = message;
            return this;
        }

        XRayExceptionBuilder withType(String type) {
            this.type = type;
            return this;
        }

        XRayExceptionBuilder withStack(List<StackElement> stack) {
            this.stack = stack;
            return this;
        }

        XRayException build() {
            return new XRayException(message, type, stack);
        }
    }
}
