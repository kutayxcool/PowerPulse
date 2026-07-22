package com.powerpulse.core.telemetry;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TelemetryPayload(
        UUID homeId,
        UUID applianceId,
        String applianceName,
        double wattage,
        OffsetDateTime timestamp
) {
}