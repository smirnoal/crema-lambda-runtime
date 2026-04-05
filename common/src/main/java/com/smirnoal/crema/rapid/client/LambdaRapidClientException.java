package com.smirnoal.crema.rapid.client;

public class LambdaRapidClientException extends RuntimeException {
    public LambdaRapidClientException(String message) {
        super(message);
    }

    public LambdaRapidClientException(String message, Throwable t) {
        super(message, t);
    }

    public LambdaRapidClientException(String message, int responseCode) {
        super(message + " Response code: '" + responseCode + "'.");
    }
}
