package com.powerpulse.core.home;

import java.util.UUID;

public class HomeNotFoundException extends RuntimeException {

    public HomeNotFoundException(UUID homeId) {
        super("Ev bulunamadı: " + homeId);
    }
}