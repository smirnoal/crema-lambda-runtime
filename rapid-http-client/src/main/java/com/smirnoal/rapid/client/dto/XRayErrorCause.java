package com.smirnoal.rapid.client.dto;

import java.util.Collection;

public class XRayErrorCause {
    public String working_directory;
    public Collection<XRayException> exceptions;
    public Collection<String> paths;

    @SuppressWarnings("unused")
    public XRayErrorCause() {

    }

    public XRayErrorCause(String working_directory, Collection<XRayException> exceptions, Collection<String> paths) {
        this.working_directory = working_directory;
        this.exceptions = exceptions;
        this.paths = paths;
    }

    public XRayErrorCause withWorkingDirectory(String workingDirectory) {
        this.working_directory = workingDirectory;
        return this;
    }

    public XRayErrorCause withExceptions(Collection<XRayException> exceptions) {
        this.exceptions = exceptions;
        return this;
    }

    public XRayErrorCause withPaths(Collection<String> paths) {
        this.paths = paths;
        return this;
    }
}
