package com.powerpulse.core.dashboard;

public class InvalidPaginationException extends RuntimeException {

    public InvalidPaginationException(String message) {
        super(message);
    }
}