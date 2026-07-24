package com.powerpulse.core.billing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyConsumptionCalculatorTest {

    private final EnergyConsumptionCalculator calculator =
            new EnergyConsumptionCalculator(10);

    @Test
    void shouldConvertWattAndTimeToKwh() {
        OffsetDateTime start =
                OffsetDateTime.parse("2026-07-22T12:00:00+03:00");

        OffsetDateTime end =
                OffsetDateTime.parse("2026-07-22T12:00:05+03:00");

        BigDecimal result = calculator.calculateKwh(
                1000,
                start,
                end
        );

        assertThat(result)
                .isEqualByComparingTo("0.0013888889");
    }

    @Test
    void shouldReturnZeroForFirstTelemetry() {
        OffsetDateTime timestamp =
                OffsetDateTime.parse("2026-07-22T12:00:00+03:00");

        BigDecimal result = calculator.calculateKwh(
                1000,
                null,
                timestamp
        );

        assertThat(result).isEqualByComparingTo("0");
    }

    @Test
    void shouldLimitLongTelemetryIntervals() {
        OffsetDateTime start =
                OffsetDateTime.parse("2026-07-22T12:00:00+03:00");

        OffsetDateTime end =
                OffsetDateTime.parse("2026-07-22T12:01:00+03:00");

        BigDecimal result = calculator.calculateKwh(
                1000,
                start,
                end
        );

        // 60 saniyelik boşluk yerine en fazla 10 saniye hesaplanır.
        assertThat(result)
                .isEqualByComparingTo("0.0027777778");
    }
}