package com.powerpulse.core.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTotalConsumptionResponse(
        LocalDate day,
        BigDecimal consumption
) {
}