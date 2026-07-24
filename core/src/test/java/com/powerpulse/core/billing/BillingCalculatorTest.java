package com.powerpulse.core.billing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BillingCalculatorTest {

    private final BillingCalculator calculator =
            new BillingCalculator();

    @Test
    void shouldCalculateNormalConsumption() {
        BillingCalculation result = calculator.calculate(
                new BigDecimal("20"),
                new BigDecimal("10"),
                new BigDecimal("100"),
                new BigDecimal("2.10")
        );

        assertThat(result.incrementalCost())
                .isEqualByComparingTo("21.00000000");

        assertThat(result.normalChargedKwh())
                .isEqualByComparingTo("10");

        assertThat(result.penaltyChargedKwh())
                .isEqualByComparingTo("0");

        assertThat(result.endingPenaltyTier()).isZero();
    }

    @Test
    void shouldChargeOnlyExceededConsumptionWithPenalty() {
        BillingCalculation result = calculator.calculate(
                new BigDecimal("90"),
                new BigDecimal("40"),
                new BigDecimal("100"),
                new BigDecimal("2.10")
        );

        /*
         * 90–100  : 10 × 2.10       = 21
         * 100–120 : 20 × 2.10 × 1.5 = 63
         * 120–130 : 10 × 2.10 × 2.0 = 42
         * Toplam                         126
         */
        assertThat(result.incrementalCost())
                .isEqualByComparingTo("126.00000000");

        assertThat(result.normalChargedKwh())
                .isEqualByComparingTo("10");

        assertThat(result.penaltyChargedKwh())
                .isEqualByComparingTo("30");

        assertThat(result.endingPenaltyTier()).isEqualTo(2);

        assertThat(result.endingMultiplier())
                .isEqualByComparingTo("2.00");
    }

    @Test
    void shouldApplyFirstPenaltyTierAtQuota() {
        BillingCalculation result = calculator.calculate(
                new BigDecimal("100"),
                new BigDecimal("10"),
                new BigDecimal("100"),
                new BigDecimal("2.10")
        );

        assertThat(result.incrementalCost())
                .isEqualByComparingTo("31.50000000");

        assertThat(result.endingPenaltyTier()).isEqualTo(1);

        assertThat(result.endingMultiplier())
                .isEqualByComparingTo("1.50");
    }
}