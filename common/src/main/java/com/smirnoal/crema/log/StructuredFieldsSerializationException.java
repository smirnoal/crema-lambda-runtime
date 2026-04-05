package com.smirnoal.crema.log;

/**
 * Raised when {@link StructuredFields} cannot be serialized into log output (JSON or TEXT).
 * {@link LambdaLogFormatter} wraps failures during field emission; sinks may strip fields and retry.
 */
public final class StructuredFieldsSerializationException extends RuntimeException {

    public StructuredFieldsSerializationException(Throwable cause) {
        super("Failed to serialize structured log fields", cause);
    }
}
