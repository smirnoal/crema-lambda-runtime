package com.smirnoal.lambda.log;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * A PrintStream that writes each complete message as a single framed telemetry entry.
 * This avoids the buffering issues of wrapping an OutputStream in PrintStream,
 * which can merge multiple log lines into one frame or split a single line across frames.
 */
public class FramedTelemetryPrintStream extends PrintStream {
    private final FramedTelemetryLogSink sink;

    public FramedTelemetryPrintStream(FramedTelemetryLogSink sink) {
        super(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);
        this.sink = sink;
    }

    @Override
    public void println(String x) {
        sink.log((x != null ? x + "\n" : "null\n").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void println(Object x) {
        println(String.valueOf(x));
    }

    @Override
    public void println() {
        sink.log("\n".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void println(boolean x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(char x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(int x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(long x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(float x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(double x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(char[] x) {
        println(new String(x));
    }

    @Override
    public void print(String s) {
        sink.log((s != null ? s : "null").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void print(Object obj) {
        print(String.valueOf(obj));
    }

    @Override
    public void print(boolean b) {
        print(String.valueOf(b));
    }

    @Override
    public void print(char c) {
        print(String.valueOf(c));
    }

    @Override
    public void print(int i) {
        print(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        print(String.valueOf(l));
    }

    @Override
    public void print(float f) {
        print(String.valueOf(f));
    }

    @Override
    public void print(double d) {
        print(String.valueOf(d));
    }

    @Override
    public void print(char[] s) {
        print(new String(s));
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        byte[] slice = new byte[len];
        System.arraycopy(buf, off, slice, 0, len);
        sink.log(slice);
    }

    @Override
    public void write(int b) {
        sink.log(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] buf) {
        sink.log(buf);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        print(String.format(format, args));
        return this;
    }

    @Override
    public PrintStream printf(java.util.Locale l, String format, Object... args) {
        print(String.format(l, format, args));
        return this;
    }
}
