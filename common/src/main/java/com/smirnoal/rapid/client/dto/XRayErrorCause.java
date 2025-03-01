package com.smirnoal.rapid.client.dto;

import java.util.Collection;

public record XRayErrorCause(
        String working_directory,
        Collection<XRayException> exceptions,
        Collection<String> paths) {

    @SuppressWarnings("unused")
    public static XRayErrorCauseBuilder builder() {
        return new XRayErrorCauseBuilder();
    }

    public static class XRayErrorCauseBuilder {
        String working_directory;
        Collection<XRayException> exceptions;
        Collection<String> paths;

        private XRayErrorCauseBuilder() {
        }

        public XRayErrorCauseBuilder withWorkingDirectory(String workingDirectory) {
            this.working_directory = workingDirectory;
            return this;
        }

        public XRayErrorCauseBuilder withExceptions(Collection<XRayException> exceptions) {
            this.exceptions = exceptions;
            return this;
        }

        public XRayErrorCauseBuilder withPaths(Collection<String> paths) {
            this.paths = paths;
            return this;
        }

        public XRayErrorCause build() {
            return new XRayErrorCause(working_directory, exceptions, paths);
        }
    }
}
