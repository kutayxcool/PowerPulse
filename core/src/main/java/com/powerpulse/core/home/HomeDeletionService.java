package com.powerpulse.core.home;

import com.powerpulse.core.ignite.IgniteUnavailableException;
import com.powerpulse.core.telemetry.LiveStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class HomeDeletionService {

    private static final Logger log =
            LoggerFactory.getLogger(HomeDeletionService.class);

    private final HomeRepository homeRepository;
    private final LiveStateStore liveStateStore;

    public HomeDeletionService(
            HomeRepository homeRepository,
            LiveStateStore liveStateStore
    ) {
        this.homeRepository = homeRepository;
        this.liveStateStore = liveStateStore;
    }

    @Transactional
    public void delete(UUID homeId, UUID ownerId) {
        Home home = homeRepository
                .findByIdAndOwnerId(homeId, ownerId)
                .orElseThrow(() -> new HomeNotFoundException(homeId));

        // Ignite'taki canlı durum temizliği en iyi çaba (best-effort)
        // ile yapılır: Ignite o an ulaşılamaz olsa bile ev silme
        // işlemi PostgreSQL tarafında (ON DELETE CASCADE ile) devam
        // etmeli, geride kalan Ignite kaydı zararsız bir çöp veridir.
        home.getAppliances().forEach(appliance -> {
            try {
                liveStateStore.removeAppliance(homeId, appliance.getId());
            } catch (IgniteUnavailableException exception) {
                log.warn(
                        "Cihazın Ignite canlı durumu silinemedi (yok sayıldı): applianceId={}",
                        appliance.getId(),
                        exception
                );
            }
        });

        try {
            liveStateStore.removeHome(homeId);
        } catch (IgniteUnavailableException exception) {
            log.warn(
                    "Evin Ignite canlı durumu silinemedi (yok sayıldı): homeId={}",
                    homeId,
                    exception
            );
        }

        // appliances, billing_ledger, notification_logs,
        // operational_events, ai_recommendations ve
        // daily_consumption_snapshots tabloları ON DELETE CASCADE
        // ile tanımlı, bu yüzden tek bir silme yeterli.
        homeRepository.delete(home);

        log.info("Ev silindi: homeId={}", homeId);
    }
}
