package com.powerpulse.core.advisory;

public record ApplianceAnomaly(
        String applianceName,
        int consecutiveBreaches
) {
}