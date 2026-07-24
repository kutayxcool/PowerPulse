package com.powerpulse.core.telemetry;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.appliance.ApplianceRepository;
import com.powerpulse.core.billing.BillingCalculation;
import com.powerpulse.core.billing.BillingCalculator;
import com.powerpulse.core.billing.BillingLedgerService;
import com.powerpulse.core.billing.EnergyConsumptionCalculator;
import com.powerpulse.core.event.OperationalEventService;
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
    private final OperationalEventService operationalEventService;
    private final BillingLedgerService billingLedgerService;

    public TelemetryProcessingService(
            ApplianceRepository applianceRepository,
            EnergyConsumptionCalculator energyCalculator,
            BillingCalculator billingCalculator,
            LiveStateStore liveStateStore,
            IgniteCacheService igniteCacheService,
            OperationalEventService operationalEventService,
            BillingLedgerService billingLedgerService
    ) {
        this.applianceRepository = applianceRepository;
        this.energyCalculator = energyCalculator;
        this.billingCalculator = billingCalculator;
        this.liveStateStore = liveStateStore;
        this.igniteCacheService = igniteCacheService;
        this.operationalEventService = operationalEventService;
        this.billingLedgerService = billingLedgerService;
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

        // Cihaz "Durdur" ile ya da zamanlayıcı bitince pasif hale
        // getirilmişse, gelen telemetri tüketime/faturaya YANSITILMAZ -
        // birikmiş tüketim olduğu gibi donar, anlık güç 0 W olarak
        // gösterilir (cihaz gerçekten "kapalı" gibi görünür). Sensors
        // modülü hâlâ sahte telemetri gönderiyor olabilir; asıl
        // durdurma etkisi burada, tek noktadan uygulanır.
        if (!appliance.isActive()) {
            Optional<ApplianceLiveState> frozenPreviousState =
                    liveStateStore.findAppliance(
                            payload.homeId(),
                            payload.applianceId()
                    );

            BigDecimal frozenConsumption = frozenPreviousState
                    .map(ApplianceLiveState::consumptionKwh)
                    .orElse(BigDecimal.ZERO.setScale(10));

            int frozenBreaches = frozenPreviousState
                    .map(ApplianceLiveState::consecutiveBreaches)
                    .orElse(0);

            liveStateStore.saveAppliance(new ApplianceLiveState(
                    home.getId(),
                    appliance.getId(),
                    appliance.getName(),
                    0.0,
                    frozenConsumption,
                    frozenBreaches,
                    false,
                    payload.timestamp()
            ));

            log.info(
                    "Cihaz durduruldu, telemetry işlenmedi (tüketim donduruldu): appliance={}",
                    appliance.getId()
            );

            return;
        }

        Optional<ApplianceLiveState> previousState =
                liveStateStore.findAppliance(
                        payload.homeId(),
                        payload.applianceId()
                );
        int previousBreachCount = previousState
                .map(ApplianceLiveState::consecutiveBreaches)
                .orElse(0);

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

        boolean isDaytimeReading = isDaytime(payload.timestamp());

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

        // Ignite'ta uygulama guncellemesinden once yazilmis eski
        // (dayConsumptionKwh/nightConsumptionKwh icermeyen) canli
        // durum kayitlari null donebilir; null-safe okunur.
        BigDecimal newDayConsumption = orZero(previousHomeState.dayConsumptionKwh())
                .add(isDaytimeReading ? addedKwh : BigDecimal.ZERO);

        BigDecimal newNightConsumption = orZero(previousHomeState.nightConsumptionKwh())
                .add(isDaytimeReading ? BigDecimal.ZERO : addedKwh);

        BigDecimal newBillAmount = previousHomeState
                .currentBillAmount()
                .add(billing.incrementalCost());

        double quotaPercentage = calculateQuotaPercentage(
                newTotalConsumption,
                home.getBudgetQuotaKwh()
        );

        BigDecimal previousApplianceConsumption = previousState
                .map(ApplianceLiveState::consumptionKwh)
                .orElse(BigDecimal.ZERO.setScale(10));

        BigDecimal newApplianceConsumption =
                previousApplianceConsumption.add(addedKwh);

        ApplianceLiveState newApplianceState =
                new ApplianceLiveState(
                        home.getId(),
                        appliance.getId(),
                        appliance.getName(),
                        payload.wattage(),
                        newApplianceConsumption,
                        breachCounter,
                        breachCounter >= 3,
                        payload.timestamp()
                );

        HomeLiveState newHomeState = new HomeLiveState(
                home.getId(),
                newTotalConsumption,
                newDayConsumption,
                newNightConsumption,
                newBillAmount,
                quotaPercentage,
                billing.endingPenaltyTier(),
                billing.endingMultiplier(),
                payload.timestamp()
        );

        // Resmî işlem sırası: önce Ignite canlı durumu güncellenir.
        liveStateStore.saveAppliance(newApplianceState);
        liveStateStore.saveHome(newHomeState);
        billingLedgerService.record(
                home,
                appliance,
                payload.timestamp(),
                billing
        );
        operationalEventService.recordTransitions(
                home,
                appliance,
                previousHomeState,
                newHomeState,
                previousBreachCount,
                breachCounter,
                payload.timestamp()
        );

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
        // Gunduz/gece kirilimi Postgres'te ayri tutulmuyor (sadece
        // toplam kalici), bu yuzden Core yeniden baslatildiginda
        // gunduz/gece sayaclari sifirdan basliyor. Toplam tuketim
        // (totalConsumptionKwh) ise Home entity'sinden geri yukleniyor.
        return new HomeLiveState(
                home.getId(),
                home.getTotalConsumptionKwh(),
                BigDecimal.ZERO.setScale(10),
                BigDecimal.ZERO.setScale(10),
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

    // Basit iki dilimli tarife: 06:00-22:00 gunduz, 22:00-06:00 gece.
    private boolean isDaytime(OffsetDateTime timestamp) {
        int hour = timestamp.getHour();
        return hour >= 6 && hour < 22;
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(10) : value;
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