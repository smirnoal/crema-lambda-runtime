package com.smirnoal.lambda.log;

import com.smirnoal.lambda.Lambda;

import java.io.FileDescriptor;
import java.io.PrintStream;
import java.lang.reflect.Constructor;

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
            PrintStream telemetryStream = new FramedTelemetryPrintStream(sink);
            System.setOut(telemetryStream);
            System.setErr(telemetryStream);
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