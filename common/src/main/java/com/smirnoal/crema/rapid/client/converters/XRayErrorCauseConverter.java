package com.smirnoal.crema.rapid.client.converters;

import com.smirnoal.crema.rapid.client.dto.StackElement;
import com.smirnoal.crema.rapid.client.dto.XRayErrorCause;
import com.smirnoal.crema.rapid.client.dto.XRayException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class XRayErrorCauseConverter {
    private XRayErrorCauseConverter() {
    }

    public static XRayErrorCause fromThrowable(Throwable throwable) {
        String workingDirectory = System.getProperty("user.dir");
        XRayException xRayException = getXRayExceptionFromThrowable(throwable);
        Collection<XRayException> exceptions = Collections.singletonList(xRayException);
        Collection<String> paths = Arrays.stream(throwable.getStackTrace()).
                map(XRayErrorCauseConverter::determineFileName).
                toList();

        return new XRayErrorCause(workingDirectory, exceptions, paths);
    }

    static XRayException getXRayExceptionFromThrowable(Throwable throwable) {
        String message = throwable.getMessage();
        String type = throwable.getClass().getName();
        List<StackElement> stack = Arrays.stream(throwable.getStackTrace()).
                map(XRayErrorCauseConverter::convertStackTraceElement).
                toList();
        return new XRayException(message, type, stack);
    }

    static String determineFileName(StackTraceElement e) {
        String fileName = null;
        if (e.getFileName() != null) {
            fileName = e.getFileName();
        }
        if (fileName == null) {
            String className = e.getClassName();
            fileName = className.substring(className.lastIndexOf('.') + 1) + ".java";
        }
        return fileName;
    }

    static StackElement convertStackTraceElement(StackTraceElement e) {
        return StackElement.builder()
                .withLabel(e.getMethodName())
                .withPath(determineFileName(e))
                .withLine(e.getLineNumber())
                .build();
    }
}
