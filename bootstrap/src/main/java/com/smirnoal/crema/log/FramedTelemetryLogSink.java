package com.smirnoal.crema.log;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;

public class FramedTelemetryLogSink implements ByteLogSink {
    private static final int HEADER_LENGTH = 16;
    private static final int FRAME_TYPE_LOG = 0xa55a0003;
    private final FileOutputStream logOutputStream;
    private final ByteBuffer headerBuf;

    public FramedTelemetryLogSink(FileDescriptor fd) {
        this.logOutputStream = new FileOutputStream(fd);
        this.headerBuf = ByteBuffer.allocate(HEADER_LENGTH).order(ByteOrder.BIG_ENDIAN);
    }

    @Override
    public synchronized void log(byte[] message) {
        try {
            updateHeader(message.length);
            this.logOutputStream.write(this.headerBuf.array());
            this.logOutputStream.write(message);
        } catch (IOException e) {
            // ignore
        }
    }

    private void updateHeader(int length) {
        this.headerBuf.clear();
        this.headerBuf.putInt(FRAME_TYPE_LOG);
        this.headerBuf.putInt(length);
        this.headerBuf.putLong(timestamp());
        this.headerBuf.flip();
    }

    private long timestamp() {
        Instant instant = Instant.now();
        return instant.getEpochSecond() * 1_000_000 + instant.getNano() / 1000;
    }
} 