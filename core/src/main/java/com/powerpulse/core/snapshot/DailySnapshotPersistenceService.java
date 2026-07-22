package com.powerpulse.core.snapshot;

import com.powerpulse.core.home.Home;
import com.powerpulse.core.home.HomeRepository;
import com.powerpulse.core.telemetry.HomeLiveState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class DailySnapshotPersistenceService {

    private static final ZoneId PROJECT_ZONE =
            ZoneId.of("Europe/Istanbul");

    private final HomeRepository homeRepository;
    private final DailyConsumptionSnapshotRepository snapshotRepository;

    public DailySnapshotPersistenceService(
            HomeRepository homeRepository,
            DailyConsumptionSnapshotRepository snapshotRepository
    ) {
        this.homeRepository = homeRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public void persist(UUID homeId, HomeLiveState liveState) {
        Home home = homeRepository
                .findById(homeId)
                .orElse(null);

        if (home == null) {
            return;
        }

        home.updateFinancialTotals(
                liveState.totalConsumptionKwh(),
                liveState.currentBillAmount()
        );

        LocalDate snapshotDate = LocalDate.now(PROJECT_ZONE);

        DailyConsumptionSnapshot snapshot = snapshotRepository
                .findByHomeIdAndSnapshotDate(homeId, snapshotDate)
                .orElseGet(() -> new DailyConsumptionSnapshot(
                        home,
                        snapshotDate,
                        liveState.totalConsumptionKwh(),
                        liveState.currentBillAmount()
                ));

        snapshot.updateValues(
                liveState.totalConsumptionKwh(),
                liveState.currentBillAmount()
        );

        snapshotRepository.save(snapshot);
    }
}