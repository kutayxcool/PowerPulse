package com.powerpulse.core.dashboard;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.dashboard.dto.ApplianceSummaryResponse;
import com.powerpulse.core.dashboard.dto.DailyConsumptionResponse;
import com.powerpulse.core.dashboard.dto.HomeDetailResponse;
import com.powerpulse.core.dashboard.dto.HomeSummaryResponse;
import com.powerpulse.core.home.Home;
import com.powerpulse.core.home.HomeNotFoundException;
import com.powerpulse.core.home.HomeRepository;
import com.powerpulse.core.snapshot.DailyConsumptionSnapshotRepository;
import com.powerpulse.core.telemetry.ApplianceLiveState;
import com.powerpulse.core.telemetry.HomeLiveState;
import com.powerpulse.core.telemetry.LiveStateStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class HomeDashboardService {

    private final HomeRepository homeRepository;
    private final LiveStateStore liveStateStore;
    private final DailyConsumptionSnapshotRepository snapshotRepository;

    public HomeDashboardService(
            HomeRepository homeRepository,
            LiveStateStore liveStateStore,
            DailyConsumptionSnapshotRepository snapshotRepository
    ) {
        this.homeRepository = homeRepository;
        this.liveStateStore = liveStateStore;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public List<HomeSummaryResponse> getHomes() {
        return homeRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public HomeDetailResponse getHome(
            UUID homeId,
            int page,
            int size
    ) {
        validatePagination(page, size);

        Home home = homeRepository
                .findById(homeId)
                .orElseThrow(() -> new HomeNotFoundException(homeId));

        HomeLiveState homeState = liveStateStore
                .findHome(homeId)
                .orElse(null);

        List<ApplianceSummaryResponse> appliances = home
                .getAppliances()
                .stream()
                .map(this::toApplianceResponse)
                .toList();

        List<DailyConsumptionResponse> dailyConsumption =
                getDailyConsumption(homeId, homeState, page, size);

        if (homeState == null) {
            return new HomeDetailResponse(
                    home.getId(),
                    home.getName(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO.setScale(2),
                    "normal",
                    appliances,
                    dailyConsumption
            );
        }

        BigDecimal quotaPercentage = BigDecimal
                .valueOf(homeState.quotaPercentage())
                .setScale(2, RoundingMode.HALF_UP);

        return new HomeDetailResponse(
                home.getId(),
                home.getName(),
                homeState.totalConsumptionKwh(),
                homeState.currentBillAmount(),
                quotaPercentage,
                determineHomeStatus(homeState.quotaPercentage()),
                appliances,
                dailyConsumption
        );
    }

    private HomeSummaryResponse toSummaryResponse(Home home) {
        return liveStateStore
                .findHome(home.getId())
                .map(state -> fromLiveState(home, state))
                .orElseGet(() -> emptyLiveState(home));
    }

    private HomeSummaryResponse fromLiveState(
            Home home,
            HomeLiveState state
    ) {
        BigDecimal quotaPercentage = BigDecimal
                .valueOf(state.quotaPercentage())
                .setScale(2, RoundingMode.HALF_UP);

        return new HomeSummaryResponse(
                home.getId(),
                home.getName(),
                state.totalConsumptionKwh(),
                state.currentBillAmount(),
                quotaPercentage,
                determineHomeStatus(state.quotaPercentage())
        );
    }

    private HomeSummaryResponse emptyLiveState(Home home) {
        return new HomeSummaryResponse(
                home.getId(),
                home.getName(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO.setScale(2),
                "normal"
        );
    }

    private ApplianceSummaryResponse toApplianceResponse(
            Appliance appliance
    ) {
        return liveStateStore
                .findAppliance(
                        appliance.getHome().getId(),
                        appliance.getId()
                )
                .map(state -> fromApplianceLiveState(appliance, state))
                .orElseGet(() -> emptyApplianceState(appliance));
    }

    private ApplianceSummaryResponse fromApplianceLiveState(
            Appliance appliance,
            ApplianceLiveState state
    ) {
        BigDecimal currentWattage = BigDecimal
                .valueOf(state.currentWattage())
                .setScale(2, RoundingMode.HALF_UP);

        return new ApplianceSummaryResponse(
                appliance.getId(),
                appliance.getName(),
                currentWattage,
                appliance.getSafeLimitWatt(),
                determineApplianceStatus(appliance, state),
                state.lastTelemetryAt()
        );
    }

    private ApplianceSummaryResponse emptyApplianceState(
            Appliance appliance
    ) {
        return new ApplianceSummaryResponse(
                appliance.getId(),
                appliance.getName(),
                BigDecimal.ZERO.setScale(2),
                appliance.getSafeLimitWatt(),
                "normal",
                null
        );
    }

    private List<DailyConsumptionResponse> getDailyConsumption(
            UUID homeId,
            HomeLiveState homeState,
            int page,
            int size
    ) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        LocalDate yesterday = today.minusDays(1);

        List<DailyConsumptionResponse> values = new ArrayList<>();

        // Bugünün tüketimi Ignite'taki canlı durumdan alınır.
        BigDecimal todayConsumption = homeState == null
                ? BigDecimal.ZERO
                : homeState.totalConsumptionKwh();

        values.add(
                new DailyConsumptionResponse(
                        today,
                        todayConsumption
                )
        );

        // Önceki günlerin tüketimleri PostgreSQL'den alınır.
        snapshotRepository
                .findByHomeIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                        homeId,
                        startDate,
                        yesterday
                )
                .stream()
                .map(snapshot -> new DailyConsumptionResponse(
                        snapshot.getSnapshotDate(),
                        snapshot.getConsumptionKwh()
                ))
                .forEach(values::add);

        int fromIndex = page * size;

        if (fromIndex >= values.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + size, values.size());

        return List.copyOf(values.subList(fromIndex, toIndex));
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidPaginationException(
                    "Sayfa numarası sıfırdan küçük olamaz."
            );
        }

        if (size < 1 || size > 100) {
            throw new InvalidPaginationException(
                    "Sayfa boyutu 1 ile 100 arasında olmalıdır."
            );
        }
    }

    private String determineHomeStatus(double quotaPercentage) {
        if (quotaPercentage > 100.0) {
            return "danger";
        }

        if (quotaPercentage >= 90.0) {
            return "warning";
        }

        return "normal";
    }

    private String determineApplianceStatus(
            Appliance appliance,
            ApplianceLiveState state
    ) {
        if (state.anomalous() || state.consecutiveBreaches() >= 3) {
            return "danger";
        }

        BigDecimal currentWattage =
                BigDecimal.valueOf(state.currentWattage());

        if (currentWattage.compareTo(appliance.getSafeLimitWatt()) > 0) {
            return "warning";
        }

        return "normal";
    }
}