package com.powerpulse.core.registration;

public record RegistrationApplianceEvent(
        String applianceId,
        String name,
        double safeLimitWatt
) {
}