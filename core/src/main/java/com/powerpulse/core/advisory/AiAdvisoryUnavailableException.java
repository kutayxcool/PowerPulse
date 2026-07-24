package com.powerpulse.core.advisory;

public class AiAdvisoryUnavailableException
        extends RuntimeException {

    public AiAdvisoryUnavailableException(String message) {
        super(message);
    }

    public AiAdvisoryUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}