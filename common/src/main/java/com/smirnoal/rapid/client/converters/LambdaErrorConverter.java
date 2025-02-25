package com.smirnoal.rapid.client.converters;



import com.smirnoal.rapid.client.dto.ErrorRequest;

import java.util.Arrays;

public class LambdaErrorConverter {
    private LambdaErrorConverter() {
    }

    public static ErrorRequest fromThrowable(Throwable throwable) {
        String errorMessage = throwable.getLocalizedMessage() == null
                ? throwable.getClass().getName()
                : throwable.getLocalizedMessage();

        String errorType = throwable.getClass().getName();

        String[] stackTrace = Arrays.stream(throwable.getStackTrace())
                .map(StackTraceElement::toString)
                .toArray(String[]::new);

        return new ErrorRequest(errorMessage, errorType, stackTrace);
    }
}
