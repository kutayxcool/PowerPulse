package com.powerpulse.core.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record HomeDetailResponse(
        UUID id,
        String name,
        BigDecimal consumption,
        BigDecimal bill,
        BigDecimal quotaPercentage,
        String status,
        List<ApplianceSummaryResponse> appliances,
        List<DailyConsumptionResponse> dailyConsumption
) {
}