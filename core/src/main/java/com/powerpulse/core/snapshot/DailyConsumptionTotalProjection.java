package com.powerpulse.core.snapshot;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyConsumptionTotalProjection {

    LocalDate getDay();

    BigDecimal getConsumption();
}