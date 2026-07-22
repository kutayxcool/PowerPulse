package com.powerpulse.core.analytics;

import com.powerpulse.core.analytics.dto.AnalyticsResponse;
import com.powerpulse.core.analytics.dto.DailyTotalConsumptionResponse;
import com.powerpulse.core.analytics.dto.HomeComparisonResponse;
import com.powerpulse.core.dashboard.InvalidPaginationException;
import com.powerpulse.core.home.Home;
import com.powerpulse.core.home.HomeRepository;
import com.powerpulse.core.snapshot.DailyConsumptionSnapshotRepository;
import com.powerpulse.core.telemetry.HomeLiveState;
import com.powerpulse.core.telemetry.LiveStateStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class AnalyticsService {

    private static final ZoneId PROJECT_ZONE =
            ZoneId.of("Europe/Istanbul");

    private final HomeRepository homeRepository;
    private final LiveStateStore liveStateStore;
    private final DailyConsumptionSnapshotRepository snapshotRepository;

    public AnalyticsService(
            HomeRepository homeRepository,
            LiveStateStore liveStateStore,
            DailyConsumptionSnapshotRepository snapshotRepository
    ) {
        this.homeRepository = homeRepository;
        this.liveStateStore = liveStateStore;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(int page, int size) {
        validatePagination(page, size);

        LocalDate today = LocalDate.now(PROJECT_ZONE);
        LocalDate startDate = today.minusDays(6);

        List<DailyTotalConsumptionResponse> dailyTotals =
                snapshotRepository
                        .findDailyTotals(startDate, today)
                        .stream()
                        .map(projection ->
                                new DailyTotalConsumptionResponse(
                                        projection.getDay(),
                                        projection.getConsumption()
                                )
                        )
                        .toList();

        Page<Home> homePage = homeRepository.findAll(
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                )
        );

        List<HomeComparisonResponse> comparisons = homePage
                .getContent()
                .stream()
                .map(this::toHomeComparison)
                .toList();

        return new AnalyticsResponse(
                dailyTotals,
                comparisons
        );
    }

    private HomeComparisonResponse toHomeComparison(Home home) {
        HomeLiveState liveState = liveStateStore
                .findHome(home.getId())
                .orElse(null);

        if (liveState == null) {
            return new HomeComparisonResponse(
                    home.getId(),
                    home.getName(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO.setScale(2)
            );
        }

        return new HomeComparisonResponse(
                home.getId(),
                home.getName(),
                liveState.totalConsumptionKwh(),
                liveState.currentBillAmount(),
                BigDecimal
                        .valueOf(liveState.quotaPercentage())
                        .setScale(2, RoundingMode.HALF_UP)
        );
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
}