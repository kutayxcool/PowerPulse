package com.powerpulse.core.advisory;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.home.Home;
import com.powerpulse.core.home.HomeNotFoundException;
import com.powerpulse.core.home.HomeRepository;
import com.powerpulse.core.notification.NotificationLogService;
import com.powerpulse.core.telemetry.ApplianceLiveState;
import com.powerpulse.core.telemetry.HomeLiveState;
import com.powerpulse.core.telemetry.LiveStateStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class HomeAdvisoryService {

    private final HomeRepository homeRepository;
    private final LiveStateStore liveStateStore;
    private final EnergyAdvisoryService energyAdvisoryService;
    private final AiRecommendationPersistenceService persistenceService;
    private final NotificationLogService notificationLogService;

    public HomeAdvisoryService(
            HomeRepository homeRepository,
            LiveStateStore liveStateStore,
            EnergyAdvisoryService energyAdvisoryService,
            AiRecommendationPersistenceService persistenceService,
            NotificationLogService notificationLogService
    ) {
        this.homeRepository = homeRepository;
        this.liveStateStore = liveStateStore;
        this.energyAdvisoryService = energyAdvisoryService;
        this.persistenceService = persistenceService;
        this.notificationLogService = notificationLogService;
    }

    public RecommendationResponse generate(UUID homeId) {
        Home home = homeRepository
                .findWithAppliancesById(homeId)
                .orElseThrow(() ->
                        new HomeNotFoundException(homeId)
                );

        HomeLiveState homeState = liveStateStore
                .findHome(homeId)
                .orElse(null);

        BigDecimal consumption = homeState == null
                ? home.getTotalConsumptionKwh()
                : homeState.totalConsumptionKwh();

        BigDecimal bill = homeState == null
                ? home.getCurrentBillAmount()
                : homeState.currentBillAmount();

        List<ApplianceAnomaly> anomalies = home
                .getAppliances()
                .stream()
                .map(appliance ->
                        findAnomaly(homeId, appliance)
                )
                .flatMap(java.util.Optional::stream)
                .toList();

        EnergyAdvisoryContext context =
                new EnergyAdvisoryContext(
                        homeId.toString(),
                        home.getName(),
                        consumption.doubleValue(),
                        home.getBudgetQuotaKwh().doubleValue(),
                        bill.doubleValue(),
                        consumption.compareTo(
                                home.getBudgetQuotaKwh()
                        ) >= 0,
                        anomalies
                );

        AdvisoryResult result =
                energyAdvisoryService.generateAdvisory(context);

        OffsetDateTime generatedAt = OffsetDateTime.now();

        persistenceService.save(
                homeId,
                result,
                generatedAt
        );

        notificationLogService.createAiRecommendationEmail(
                home,
                result.title(),
                buildEmailMessage(home, result)
        );

        return new RecommendationResponse(
                result.title(),
                home.getId(),
                home.getName(),
                List.copyOf(result.recommendations()),
                BigDecimal.valueOf(
                        result.estimatedSavingPercentage()
                ),
                BigDecimal.valueOf(
                        result.estimatedSavingAmount()
                ),
                generatedAt
        );
    }

    private java.util.Optional<ApplianceAnomaly> findAnomaly(
            UUID homeId,
            Appliance appliance
    ) {
        return liveStateStore
                .findAppliance(homeId, appliance.getId())
                .filter(state ->
                        state.anomalous()
                                || state.consecutiveBreaches() >= 3
                )
                .map(state -> toAnomaly(appliance, state));
    }

    private ApplianceAnomaly toAnomaly(
            Appliance appliance,
            ApplianceLiveState state
    ) {
        return new ApplianceAnomaly(
                appliance.getName(),
                state.consecutiveBreaches()
        );
    }

    private String buildEmailMessage(
            Home home,
            AdvisoryResult result
    ) {
        String recommendations = result
                .recommendations()
                .stream()
                .map(recommendation -> "- " + recommendation)
                .collect(
                        java.util.stream.Collectors.joining(
                                System.lineSeparator()
                        )
                );

        return """
                Merhaba,

                %s için PowerPulse enerji tavsiyeleri:

                %s

                Tahmini tasarruf oranı: %.2f%%
                Tahmini tasarruf miktarı: %.2f TL
                """
                .formatted(
                        home.getName(),
                        recommendations,
                        result.estimatedSavingPercentage(),
                        result.estimatedSavingAmount()
                );
    }
}