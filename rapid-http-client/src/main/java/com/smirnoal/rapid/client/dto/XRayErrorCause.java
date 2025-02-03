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
}
