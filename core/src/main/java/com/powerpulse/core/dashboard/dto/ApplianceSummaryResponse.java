package com.powerpulse.core.dashboard.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplianceSummaryResponse(
        UUID id,
        String name,
        BigDecimal watt,
        BigDecimal safeLimitWatt,
        String status,
        OffsetDateTime lastTelemetryAt
) {
}