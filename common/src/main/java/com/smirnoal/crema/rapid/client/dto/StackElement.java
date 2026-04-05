package com.smirnoal.crema.rapid.client.dto;

public record StackElement(String label,
                           String path,
                           int line) {

    public static StackElementBuilder builder() {
        return new StackElementBuilder();
    }

    public static class StackElementBuilder {
        String label;
        String path;
        int line;

        private StackElementBuilder() {
        }

        public StackElementBuilder withLabel(String label) {
            this.label = label;
            return this;
        }

        public StackElementBuilder withPath(String path) {
            this.path = path;
            return this;
        }

        public StackElementBuilder withLine(int line) {
            this.line = line;
            return this;
        }

        public StackElement build() {
            return new StackElement(label, path, line);
        }
    }
}
