package com.powerpulse.core.advisory;

import java.util.List;

public record EnergyAdvisoryContext(
        String homeId,
        String homeName,
        double totalConsumptionKwh,
        double budgetQuotaKwh,
        double currentBillAmount,
        boolean quotaBreached,
        List<ApplianceAnomaly> anomalies
) {
}