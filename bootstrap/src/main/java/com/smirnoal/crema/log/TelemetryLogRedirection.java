package com.smirnoal.crema.log;

import com.smirnoal.crema.Lambda;
import com.smirnoal.crema.log.RicLog.RicLogger;

import java.io.FileDescriptor;
import java.io.PrintStream;
import java.lang.reflect.Constructor;

public class TelemetryLogRedirection {

    private static final RicLogger log = RicLog.getLogger("main");

    public static void setupIfAvailable() {
        String telemetryFdStr = Lambda.Environment.LAMBDA_TELEMETRY_LOG_FD;
        if (telemetryFdStr == null || !trySetupTelemetryFd(telemetryFdStr)) {
            setupStderrFallback();
        }
    }

    private static boolean trySetupTelemetryFd(String telemetryFdStr) {
        try {
            int fd = Integer.parseInt(telemetryFdStr);
            FileDescriptor fileDescriptor = getFileDescriptor(fd);
            FramedTelemetryLogSink framedSink = new FramedTelemetryLogSink(fileDescriptor);
            FormattingLogSink formatSink = new FormattingLogSink(framedSink);
            PrintStream telemetryStream = new FramedTelemetryPrintStream(formatSink);

            System.setOut(telemetryStream);
            System.setErr(telemetryStream);
            LambdaLogSinkHolder.set(formatSink);
            return true;
        } catch (NumberFormatException | ReflectiveOperationException e) {
            log.exception("Telemetry setup failed, falling back to stderr", e);
            return false;
        }
    }

    /** Uses stderr when telemetry FD is unset or setup failed. */
    private static void setupStderrFallback() {
        ByteLogSink fallbackSink = new StderrByteSink();
        FormattingLogSink formatSink = new FormattingLogSink(fallbackSink);
        LambdaLogSinkHolder.set(formatSink);
    }

    private static FileDescriptor getFileDescriptor(int fd) throws ReflectiveOperationException {
        Constructor<FileDescriptor> fdConstructor = FileDescriptor.class.getDeclaredConstructor(int.class);
        fdConstructor.setAccessible(true);
        return fdConstructor.newInstance(fd);
    }
} 