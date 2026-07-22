package com.powerpulse.core.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HomeComparisonResponse(
        UUID homeId,
        String homeName,
        BigDecimal consumption,
        BigDecimal bill,
        BigDecimal quotaPercentage
) {
}