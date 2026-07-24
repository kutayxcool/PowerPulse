package com.powerpulse.core.auth;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-posta veya şifre hatalı.");
    }
}
