package com.smirnoal.rapid.client.dto;

public class StackElement {
    public String label;
    public String path;
    public int line;

    @SuppressWarnings("unused")
    public StackElement() {
    }

    public StackElement(String label, String path, int line) {
        this.label = label;
        this.path = path;
        this.line = line;
    }
}
