package com.powerpulse.core.telemetry;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplianceLiveState(
        UUID homeId,
        UUID applianceId,
        String applianceName,
        double currentWattage,
        int consecutiveBreaches,
        boolean anomalous,
        OffsetDateTime lastTelemetryAt
) {
}