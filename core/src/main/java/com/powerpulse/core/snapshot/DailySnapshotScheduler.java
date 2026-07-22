package com.powerpulse.core.snapshot;

import com.powerpulse.core.home.Home;
import com.powerpulse.core.home.HomeRepository;
import com.powerpulse.core.telemetry.LiveStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailySnapshotScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(DailySnapshotScheduler.class);

    private final HomeRepository homeRepository;
    private final LiveStateStore liveStateStore;
    private final DailySnapshotPersistenceService persistenceService;

    public DailySnapshotScheduler(
            HomeRepository homeRepository,
            LiveStateStore liveStateStore,
            DailySnapshotPersistenceService persistenceService
    ) {
        this.homeRepository = homeRepository;
        this.liveStateStore = liveStateStore;
        this.persistenceService = persistenceService;
    }

    @Scheduled(
            fixedDelayString =
                    "${powerpulse.snapshot.interval-ms:900000}",
            initialDelayString =
                    "${powerpulse.snapshot.initial-delay-ms:60000}"
    )
    public void captureSnapshots() {
        log.info("Günlük tüketim snapshot işlemi başladı.");

        int savedSnapshotCount = 0;

        for (Home home : homeRepository.findAll()) {
            try {
                var liveState = liveStateStore.findHome(home.getId());

                if (liveState.isEmpty()) {
                    continue;
                }

                persistenceService.persist(
                        home.getId(),
                        liveState.get()
                );

                savedSnapshotCount++;
            } catch (RuntimeException exception) {
                log.error(
                        "Ev snapshot kaydı oluşturulamadı. homeId={}",
                        home.getId(),
                        exception
                );
            }
        }

        log.info(
                "Günlük tüketim snapshot işlemi tamamlandı. kaydedilen={}",
                savedSnapshotCount
        );
    }
}