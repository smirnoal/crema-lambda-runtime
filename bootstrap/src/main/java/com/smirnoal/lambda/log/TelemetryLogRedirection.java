package com.smirnoal.lambda.log;

import com.smirnoal.lambda.Lambda;

import java.io.FileDescriptor;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;

public class TelemetryLogRedirection {
    public static void setupIfAvailable() {
        String telemetryFdStr = Lambda.Environment.LAMBDA_TELEMETRY_LOG_FD;
        if (telemetryFdStr == null) {
            return;
        }
        try {
            int fd = Integer.parseInt(telemetryFdStr);
            FileDescriptor fileDescriptor = getFileDescriptor(fd);
            FramedTelemetryLogSink sink = new FramedTelemetryLogSink(fileDescriptor);
            OutputStream out = new FramedTelemetryOutputStream(sink);
            System.setOut(new PrintStream(out, false, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(out, false, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static FileDescriptor getFileDescriptor(int fd) throws Exception {
        Constructor<FileDescriptor> fdConstructor = FileDescriptor.class.getDeclaredConstructor(int.class);
        fdConstructor.setAccessible(true);
        return fdConstructor.newInstance(fd);
    }
} 