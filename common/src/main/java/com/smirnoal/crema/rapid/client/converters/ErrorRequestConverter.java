package com.smirnoal.crema.rapid.client.converters;



import com.smirnoal.crema.rapid.client.dto.ErrorRequest;

import java.util.Arrays;

public class ErrorRequestConverter {
    private ErrorRequestConverter() {
    }

    public static ErrorRequest fromThrowable(Throwable throwable) {
        return fromThrowable(throwable, throwable.getClass().getName());
    }

    public static ErrorRequest fromThrowable(Throwable throwable, String errorType) {
        String errorMessage = throwable.getLocalizedMessage() == null
                ? throwable.getClass().getName()
                : throwable.getLocalizedMessage();

        String[] stackTrace = Arrays.stream(throwable.getStackTrace())
                .map(StackTraceElement::toString)
                .toArray(String[]::new);

        return new ErrorRequest(errorMessage, errorType, stackTrace);
    }
}
