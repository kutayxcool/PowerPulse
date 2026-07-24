package com.powerpulse.core.telemetry;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplianceLiveState(
        UUID homeId,
        UUID applianceId,
        String applianceName,
        double currentWattage,
        BigDecimal consumptionKwh,
        int consecutiveBreaches,
        boolean anomalous,
        OffsetDateTime lastTelemetryAt
) {
}