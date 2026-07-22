package com.powerpulse.core.telemetry;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HomeLiveState(
        UUID homeId,
        BigDecimal totalConsumptionKwh,
        BigDecimal currentBillAmount,
        double quotaPercentage,
        int penaltyTier,
        double penaltyMultiplier,
        OffsetDateTime updatedAt
) {
}