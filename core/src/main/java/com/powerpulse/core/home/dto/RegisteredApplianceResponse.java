package com.powerpulse.core.home.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisteredApplianceResponse(
        UUID id,
        String name,
        BigDecimal safeLimitWatt
) {
}