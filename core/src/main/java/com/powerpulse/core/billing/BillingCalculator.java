package com.powerpulse.core.billing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BillingCalculator {

    private static final BigDecimal PENALTY_STEP =
            new BigDecimal("0.20");

    private static final BigDecimal MULTIPLIER_STEP =
            new BigDecimal("0.50");

    private static final int CALCULATION_SCALE = 8;

    public BillingCalculation calculate(
            BigDecimal previousConsumptionKwh,
            BigDecimal addedConsumptionKwh,
            BigDecimal quotaKwh,
            BigDecimal baseRatePerKwh
    ) {
        validate(
                previousConsumptionKwh,
                addedConsumptionKwh,
                quotaKwh,
                baseRatePerKwh
        );

        BigDecimal endingConsumption =
                previousConsumptionKwh.add(addedConsumptionKwh);

        BigDecimal current = previousConsumptionKwh;
        BigDecimal normalKwh = BigDecimal.ZERO;
        BigDecimal penaltyKwh = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;

        while (current.compareTo(endingConsumption) < 0) {
            int tier = determineTier(current, quotaKwh);
            BigDecimal multiplier = multiplierForTier(tier);
            BigDecimal boundary = nextBoundary(current, quotaKwh, tier);

            BigDecimal segmentEnd = boundary.min(endingConsumption);
            BigDecimal segmentKwh = segmentEnd.subtract(current);

            BigDecimal segmentCost = segmentKwh
                    .multiply(baseRatePerKwh)
                    .multiply(multiplier);

            cost = cost.add(segmentCost);

            if (tier == 0) {
                normalKwh = normalKwh.add(segmentKwh);
            } else {
                penaltyKwh = penaltyKwh.add(segmentKwh);
            }

            current = segmentEnd;
        }

        int endingTier = determineTier(endingConsumption, quotaKwh);

        return new BillingCalculation(
                addedConsumptionKwh,
                normalKwh,
                penaltyKwh,
                cost.setScale(CALCULATION_SCALE, RoundingMode.HALF_UP),
                endingTier,
                multiplierForTier(endingTier)
        );
    }

    private int determineTier(
            BigDecimal consumptionKwh,
            BigDecimal quotaKwh
    ) {
        if (consumptionKwh.compareTo(quotaKwh) < 0) {
            return 0;
        }

        BigDecimal tierSize = quotaKwh.multiply(PENALTY_STEP);
        BigDecimal excess = consumptionKwh.subtract(quotaKwh);

        return excess
                .divide(tierSize, 0, RoundingMode.FLOOR)
                .intValueExact() + 1;
    }

    private BigDecimal multiplierForTier(int tier) {
        if (tier == 0) {
            return BigDecimal.ONE;
        }

        return BigDecimal.ONE.add(
                MULTIPLIER_STEP.multiply(BigDecimal.valueOf(tier))
        );
    }

    private BigDecimal nextBoundary(
            BigDecimal currentConsumption,
            BigDecimal quotaKwh,
            int tier
    ) {
        if (tier == 0) {
            return quotaKwh;
        }

        BigDecimal tierSize = quotaKwh.multiply(PENALTY_STEP);

        return quotaKwh.add(
                tierSize.multiply(BigDecimal.valueOf(tier))
        );
    }

    private void validate(
            BigDecimal previousConsumption,
            BigDecimal addedConsumption,
            BigDecimal quota,
            BigDecimal baseRate
    ) {
        if (previousConsumption == null
                || addedConsumption == null
                || quota == null
                || baseRate == null) {
            throw new IllegalArgumentException(
                    "Fatura hesaplama değerleri null olamaz."
            );
        }

        if (previousConsumption.signum() < 0
                || addedConsumption.signum() < 0) {
            throw new IllegalArgumentException(
                    "Tüketim değerleri negatif olamaz."
            );
        }

        if (quota.signum() <= 0 || baseRate.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Kota ve temel tarife sıfırdan büyük olmalıdır."
            );
        }
    }
}