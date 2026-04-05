package com.smirnoal.crema.log;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * A PrintStream that writes each complete message as a single framed telemetry entry.
 * Uses FormattingLogSink to apply JSON/TEXT formatting and level filtering.
 */
public class FramedTelemetryPrintStream extends PrintStream {
    private final FormattingLogSink formatSink;

    public FramedTelemetryPrintStream(FormattingLogSink formatSink) {
        super(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);
        this.formatSink = formatSink;
    }

    @Override
    public void println(String x) {
        formatSink.log(LogLevel.UNDEFINED, x != null ? x : "null", null, null);
    }

    @Override
    public void println(Object x) {
        println(String.valueOf(x));
    }

    @Override
    public void println() {
        formatSink.log(LogLevel.UNDEFINED, "", null, null);
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
        formatSink.log(LogLevel.UNDEFINED, s != null ? s : "null", null, null);
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
        formatSink.logRaw(slice);
    }

    @Override
    public void write(int b) {
        formatSink.logRaw(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] buf) {
        formatSink.logRaw(buf != null ? buf : new byte[0]);
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
