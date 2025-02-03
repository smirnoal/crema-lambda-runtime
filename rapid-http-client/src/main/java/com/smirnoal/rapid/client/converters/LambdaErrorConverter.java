package com.smirnoal.rapid.client.converters;


import com.smirnoal.rapid.client.dto.ErrorRequest;

public class LambdaErrorConverter {
    private LambdaErrorConverter() {
    }

    public static ErrorRequest fromThrowable(Throwable throwable) {
        String errorMessage = throwable.getLocalizedMessage() == null
            ? throwable.getClass().getName()
            : throwable.getLocalizedMessage();
        String errorType = throwable.getClass().getName();

        StackTraceElement[] trace = throwable.getStackTrace();
        String[] stackTrace = new String[trace.length];
        for (int i = 0; i < trace.length; i++) {
            stackTrace[i] = trace[i].toString();
        }
        return new ErrorRequest(errorMessage, errorType, stackTrace);
    }
}
