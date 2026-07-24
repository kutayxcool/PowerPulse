package com.powerpulse.core.billing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;

@Component
public class EnergyConsumptionCalculator {

    private static final BigDecimal WATT_SECONDS_PER_KWH =
            new BigDecimal("3600000");

    private static final int ENERGY_SCALE = 10;

    private final long maxIntervalSeconds;

    public EnergyConsumptionCalculator(
            @Value("${powerpulse.telemetry.max-interval-seconds:10}")
            long maxIntervalSeconds
    ) {
        if (maxIntervalSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Maksimum telemetry aralığı pozitif olmalıdır."
            );
        }

        this.maxIntervalSeconds = maxIntervalSeconds;
    }

    public BigDecimal calculateKwh(
            double wattage,
            OffsetDateTime previousTimestamp,
            OffsetDateTime currentTimestamp
    ) {
        if (wattage < 0) {
            throw new IllegalArgumentException(
                    "Watt değeri negatif olamaz."
            );
        }

        if (currentTimestamp == null) {
            throw new IllegalArgumentException(
                    "Telemetry zamanı boş olamaz."
            );
        }

        // İlk ölçümde önceki zaman bilinmediği için tüketim yazılmaz.
        if (previousTimestamp == null) {
            return BigDecimal.ZERO.setScale(ENERGY_SCALE);
        }

        long elapsedMilliseconds = Duration.between(
                previousTimestamp,
                currentTimestamp
        ).toMillis();

        // Eski veya aynı zamanlı mesaj tekrar ücretlendirilmez.
        if (elapsedMilliseconds <= 0) {
            return BigDecimal.ZERO.setScale(ENERGY_SCALE);
        }

        long maximumMilliseconds = maxIntervalSeconds * 1000;
        long billableMilliseconds = Math.min(
                elapsedMilliseconds,
                maximumMilliseconds
        );

        BigDecimal wattSeconds = BigDecimal
                .valueOf(wattage)
                .multiply(BigDecimal.valueOf(billableMilliseconds))
                .divide(BigDecimal.valueOf(1000), ENERGY_SCALE,
                        RoundingMode.HALF_UP);

        return wattSeconds.divide(
                WATT_SECONDS_PER_KWH,
                ENERGY_SCALE,
                RoundingMode.HALF_UP
        );
    }
}