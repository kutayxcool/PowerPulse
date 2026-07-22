package com.powerpulse.core.snapshot;

import com.powerpulse.core.billing.BillingCalculation;
import com.powerpulse.core.billing.BillingCalculator;
import com.powerpulse.core.home.Home;
import com.powerpulse.core.home.HomeRepository;
import com.powerpulse.core.ignite.IgniteUnavailableException;
import com.powerpulse.core.telemetry.HomeLiveState;
import com.powerpulse.core.telemetry.LiveStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class IgniteStateRecoveryService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    IgniteStateRecoveryService.class
            );

    private final HomeRepository homeRepository;
    private final DailyConsumptionSnapshotRepository snapshotRepository;
    private final LiveStateStore liveStateStore;
    private final BillingCalculator billingCalculator;

    public IgniteStateRecoveryService(
            HomeRepository homeRepository,
            DailyConsumptionSnapshotRepository snapshotRepository,
            LiveStateStore liveStateStore,
            BillingCalculator billingCalculator
    ) {
        this.homeRepository = homeRepository;
        this.snapshotRepository = snapshotRepository;
        this.liveStateStore = liveStateStore;
        this.billingCalculator = billingCalculator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverMissingHomeStates() {
        log.info(
                "Ignite canlı durum kurtarma işlemi başladı."
        );

        int recoveredCount = 0;
        int existingCount = 0;
        int missingSnapshotCount = 0;

        for (Home home : homeRepository.findAll()) {
            try {
                if (liveStateStore
                        .findHome(home.getId())
                        .isPresent()) {
                    existingCount++;
                    continue;
                }

                var snapshot = snapshotRepository
                        .findTopByHomeIdOrderBySnapshotDateDesc(
                                home.getId()
                        );

                if (snapshot.isEmpty()) {
                    missingSnapshotCount++;

                    log.info(
                            "Ev için kurtarılabilir snapshot bulunamadı. homeId={}",
                            home.getId()
                    );

                    continue;
                }

                DailyConsumptionSnapshot latestSnapshot =
                        snapshot.get();

                BillingCalculation billing =
                        billingCalculator.calculate(
                                latestSnapshot
                                        .getConsumptionKwh(),
                                BigDecimal.ZERO,
                                home.getBudgetQuotaKwh(),
                                home.getBaseRatePerKwh()
                        );

                double quotaPercentage =
                        calculateQuotaPercentage(
                                latestSnapshot
                                        .getConsumptionKwh(),
                                home.getBudgetQuotaKwh()
                        );

                HomeLiveState recoveredState =
                        new HomeLiveState(
                                home.getId(),
                                latestSnapshot
                                        .getConsumptionKwh(),
                                latestSnapshot
                                        .getBillAmount(),
                                quotaPercentage,
                                billing.endingPenaltyTier(),
                                billing.endingMultiplier(),
                                latestSnapshot.getCreatedAt()
                        );

                liveStateStore.saveHome(recoveredState);
                recoveredCount++;

                log.info(
                        "Ev canlı durumu PostgreSQL snapshot'ından kurtarıldı. homeId={}, snapshotDate={}",
                        home.getId(),
                        latestSnapshot.getSnapshotDate()
                );
            } catch (IgniteUnavailableException exception) {
                log.error(
                        "Ignite kullanılamadığı için kurtarma işlemi durduruldu.",
                        exception
                );

                return;
            } catch (RuntimeException exception) {
                log.error(
                        "Ev canlı durumu kurtarılamadı. homeId={}",
                        home.getId(),
                        exception
                );
            }
        }

        log.info(
                "Ignite canlı durum kurtarma işlemi tamamlandı. kurtarılan={}, mevcut={}, snapshotYok={}",
                recoveredCount,
                existingCount,
                missingSnapshotCount
        );
    }

    private double calculateQuotaPercentage(
            BigDecimal consumption,
            BigDecimal quota
    ) {
        return consumption
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        quota,
                        2,
                        RoundingMode.HALF_UP
                )
                .doubleValue();
    }
}