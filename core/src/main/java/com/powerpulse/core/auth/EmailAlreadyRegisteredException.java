package com.powerpulse.core.auth;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("Bu e-posta adresi zaten kayıtlı: " + email);
    }
}
