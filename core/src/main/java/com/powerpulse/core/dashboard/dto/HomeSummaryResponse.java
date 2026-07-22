package com.powerpulse.core.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HomeSummaryResponse(
        UUID id,
        String name,
        BigDecimal consumption,
        BigDecimal bill,
        BigDecimal quotaPercentage,
        String status
) {
}