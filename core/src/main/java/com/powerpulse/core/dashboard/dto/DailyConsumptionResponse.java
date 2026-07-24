package com.powerpulse.core.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyConsumptionResponse(
        LocalDate day,
        BigDecimal consumption
) {
}