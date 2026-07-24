package com.powerpulse.core.telemetry;

public class LiveStateSerializationException extends RuntimeException {

    public LiveStateSerializationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}