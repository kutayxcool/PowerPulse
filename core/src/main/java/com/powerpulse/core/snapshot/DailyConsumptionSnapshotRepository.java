package com.powerpulse.core.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyConsumptionSnapshotRepository
        extends JpaRepository<DailyConsumptionSnapshot, Long> {

    List<DailyConsumptionSnapshot>
    findByHomeIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
            UUID homeId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<DailyConsumptionSnapshot> findByHomeIdAndSnapshotDate(
            UUID homeId,
            LocalDate snapshotDate
    );
}