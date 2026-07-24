package com.powerpulse.core.advisory;

public interface EnergyAdvisoryService {

    AdvisoryResult generateAdvisory(
            EnergyAdvisoryContext context
    );
}