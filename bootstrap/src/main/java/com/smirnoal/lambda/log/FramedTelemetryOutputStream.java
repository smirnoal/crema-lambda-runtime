package com.smirnoal.lambda.log;

import java.io.OutputStream;

public class FramedTelemetryOutputStream extends OutputStream {
    private final FramedTelemetryLogSink sink;

    public FramedTelemetryOutputStream(FramedTelemetryLogSink sink) {
        this.sink = sink;
    }

    @Override
    public void write(int b) {
        sink.log(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (off == 0 && len == b.length) {
            sink.log(b);
        } else {
            byte[] slice = new byte[len];
            System.arraycopy(b, off, slice, 0, len);
            sink.log(slice);
        }
    }
} 