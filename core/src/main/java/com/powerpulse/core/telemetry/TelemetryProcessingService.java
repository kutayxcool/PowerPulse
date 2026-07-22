package com.powerpulse.core.telemetry;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.appliance.ApplianceRepository;
import com.powerpulse.core.billing.BillingCalculation;
import com.powerpulse.core.billing.BillingCalculator;
import com.powerpulse.core.billing.EnergyConsumptionCalculator;
import com.powerpulse.core.home.Home;
import com.powerpulse.core.ignite.IgniteCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class TelemetryProcessingService {

    private static final Logger log =
            LoggerFactory.getLogger(TelemetryProcessingService.class);

    private final ApplianceRepository applianceRepository;
    private final EnergyConsumptionCalculator energyCalculator;
    private final BillingCalculator billingCalculator;
    private final LiveStateStore liveStateStore;
    private final IgniteCacheService igniteCacheService;

    public TelemetryProcessingService(
            ApplianceRepository applianceRepository,
            EnergyConsumptionCalculator energyCalculator,
            BillingCalculator billingCalculator,
            LiveStateStore liveStateStore,
            IgniteCacheService igniteCacheService
    ) {
        this.applianceRepository = applianceRepository;
        this.energyCalculator = energyCalculator;
        this.billingCalculator = billingCalculator;
        this.liveStateStore = liveStateStore;
        this.igniteCacheService = igniteCacheService;
    }

    public void process(TelemetryPayload payload) {
        Appliance appliance = applianceRepository
                .findById(payload.applianceId())
                .orElse(null);

        if (appliance == null) {
            log.warn(
                    "Kayıtlı olmayan cihaz için telemetry alındı: {}",
                    payload.applianceId()
            );
            return;
        }

        Home home = appliance.getHome();

        if (!home.getId().equals(payload.homeId())) {
            log.warn(
                    "Telemetry homeId ile cihazın kayıtlı homeId değeri uyuşmuyor: {}",
                    payload.applianceId()
            );
            return;
        }

        Optional<ApplianceLiveState> previousState =
                liveStateStore.findAppliance(
                        payload.homeId(),
                        payload.applianceId()
                );

        if (isOldOrDuplicate(payload, previousState)) {
            log.warn(
                    "Eski veya tekrarlanan telemetry mesajı atlandı: {}",
                    payload.applianceId()
            );
            return;
        }

        OffsetDateTime previousTimestamp = previousState
                .map(ApplianceLiveState::lastTelemetryAt)
                .orElse(null);

        BigDecimal addedKwh = energyCalculator.calculateKwh(
                payload.wattage(),
                previousTimestamp,
                payload.timestamp()
        );

        HomeLiveState previousHomeState = liveStateStore
                .findHome(home.getId())
                .orElseGet(() -> initialHomeState(home));

        BillingCalculation billing = billingCalculator.calculate(
                previousHomeState.totalConsumptionKwh(),
                addedKwh,
                home.getBudgetQuotaKwh(),
                home.getBaseRatePerKwh()
        );

        int breachCounter = calculateBreachCounter(
                appliance,
                payload.wattage()
        );

        BigDecimal newTotalConsumption = previousHomeState
                .totalConsumptionKwh()
                .add(addedKwh);

        BigDecimal newBillAmount = previousHomeState
                .currentBillAmount()
                .add(billing.incrementalCost());

        double quotaPercentage = calculateQuotaPercentage(
                newTotalConsumption,
                home.getBudgetQuotaKwh()
        );

        ApplianceLiveState newApplianceState =
                new ApplianceLiveState(
                        home.getId(),
                        appliance.getId(),
                        appliance.getName(),
                        payload.wattage(),
                        breachCounter,
                        breachCounter >= 3,
                        payload.timestamp()
                );

        HomeLiveState newHomeState = new HomeLiveState(
                home.getId(),
                newTotalConsumption,
                newBillAmount,
                quotaPercentage,
                billing.endingPenaltyTier(),
                billing.endingMultiplier(),
                payload.timestamp()
        );

        // Resmî işlem sırası: önce Ignite canlı durumu güncellenir.
        liveStateStore.saveAppliance(newApplianceState);
        liveStateStore.saveHome(newHomeState);

        log.info(
                "Telemetry işlendi: home={}, appliance={}, watt={}, addedKwh={}, tier={}",
                home.getId(),
                appliance.getId(),
                payload.wattage(),
                addedKwh,
                billing.endingPenaltyTier()
        );
    }

    private int calculateBreachCounter(
            Appliance appliance,
            double wattage
    ) {
        String key = appliance.getHome().getId()
                + ":" + appliance.getId();

        if (BigDecimal.valueOf(wattage)
                .compareTo(appliance.getSafeLimitWatt()) <= 0) {
            igniteCacheService.removeCounter(key);
            return 0;
        }

        int newValue = igniteCacheService.getCounter(key) + 1;
        igniteCacheService.putCounter(key, newValue);

        if (newValue == 3) {
            log.warn(
                    "Cihaz anomalisi tespit edildi: appliance={}",
                    appliance.getId()
            );
        }

        return newValue;
    }

    private boolean isOldOrDuplicate(
            TelemetryPayload payload,
            Optional<ApplianceLiveState> previousState
    ) {
        return previousState
                .map(ApplianceLiveState::lastTelemetryAt)
                .map(previousTime ->
                        !payload.timestamp().isAfter(previousTime))
                .orElse(false);
    }

    private HomeLiveState initialHomeState(Home home) {
        return new HomeLiveState(
                home.getId(),
                home.getTotalConsumptionKwh(),
                home.getCurrentBillAmount(),
                calculateQuotaPercentage(
                        home.getTotalConsumptionKwh(),
                        home.getBudgetQuotaKwh()
                ),
                0,
                BigDecimal.ONE,
                home.getUpdatedAt()
        );
    }

    private double calculateQuotaPercentage(
            BigDecimal consumption,
            BigDecimal quota
    ) {
        return consumption
                .multiply(BigDecimal.valueOf(100))
                .divide(quota, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}