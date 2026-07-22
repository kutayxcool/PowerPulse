package com.powerpulse.core.billing;

import java.math.BigDecimal;

public record BillingCalculation(
        BigDecimal addedConsumptionKwh,
        BigDecimal normalChargedKwh,
        BigDecimal penaltyChargedKwh,
        BigDecimal incrementalCost,
        int endingPenaltyTier,
        BigDecimal endingMultiplier
) {
}