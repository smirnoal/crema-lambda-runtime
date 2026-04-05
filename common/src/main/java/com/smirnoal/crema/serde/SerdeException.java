package com.smirnoal.crema.serde;

/**
 * Thrown when serialization or deserialization fails in a {@link LambdaSerde} implementation.
 */
public class SerdeException extends RuntimeException {

    public SerdeException(String message) {
        super(message);
    }

    public SerdeException(String message, Throwable cause) {
        super(message, cause);
    }
}
