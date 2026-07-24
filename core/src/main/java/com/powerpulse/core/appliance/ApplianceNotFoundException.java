package com.powerpulse.core.appliance;

import java.util.UUID;

public class ApplianceNotFoundException extends RuntimeException {

    public ApplianceNotFoundException(UUID applianceId) {
        super("Cihaz bulunamadı: " + applianceId);
    }
}
