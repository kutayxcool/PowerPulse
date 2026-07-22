package com.powerpulse.core.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    Optional<DailyConsumptionSnapshot>
    findTopByHomeIdOrderBySnapshotDateDesc(UUID homeId);
    @Query("""
            SELECT
                snapshot.snapshotDate AS day,
                SUM(snapshot.consumptionKwh) AS consumption
            FROM DailyConsumptionSnapshot snapshot
            WHERE snapshot.snapshotDate BETWEEN :startDate AND :endDate
            GROUP BY snapshot.snapshotDate
            ORDER BY snapshot.snapshotDate ASC
            """)
    List<DailyConsumptionTotalProjection> findDailyTotals(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}