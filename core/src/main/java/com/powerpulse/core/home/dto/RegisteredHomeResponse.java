package com.powerpulse.core.home.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RegisteredHomeResponse(
        UUID id,
        String name,
        String contactEmail,
        BigDecimal budgetQuotaKwh,
        BigDecimal baseRatePerKwh,
        BigDecimal totalConsumptionKwh,
        BigDecimal currentBillAmount,
        List<RegisteredApplianceResponse> appliances,
        OffsetDateTime createdAt
) {
}