package com.powerpulse.core.registration;

import java.util.List;

public record HomeRegistrationEvent(
        String homeId,
        String contactEmail,
        double budgetQuotaKwh,
        List<RegistrationApplianceEvent> appliances
) {
}