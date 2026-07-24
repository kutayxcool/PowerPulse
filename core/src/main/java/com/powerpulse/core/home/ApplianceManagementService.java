package com.powerpulse.core.home;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.appliance.ApplianceNotFoundException;
import com.powerpulse.core.appliance.ApplianceRepository;
import com.powerpulse.core.home.dto.ApplianceStatusResponse;
import com.powerpulse.core.home.dto.RegisterApplianceRequest;
import com.powerpulse.core.home.dto.RegisteredApplianceResponse;
import com.powerpulse.core.ignite.IgniteUnavailableException;
import com.powerpulse.core.telemetry.LiveStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ApplianceManagementService {

    private static final Logger log =
            LoggerFactory.getLogger(ApplianceManagementService.class);

    private final HomeRepository homeRepository;
    private final ApplianceRepository applianceRepository;
    private final LiveStateStore liveStateStore;

    public ApplianceManagementService(
            HomeRepository homeRepository,
            ApplianceRepository applianceRepository,
            LiveStateStore liveStateStore
    ) {
        this.homeRepository = homeRepository;
        this.applianceRepository = applianceRepository;
        this.liveStateStore = liveStateStore;
    }

    // Not: Bu, sadece PostgreSQL kaydını günceller. Sensors modülü,
    // /api/internal/homes uzerinden periyodik olarak (varsayılan 15
    // saniyede bir) Core'daki güncel ev/cihaz listesini çektiği için
    // burada eklenen yeni cihazlar da bir sonraki senkronizasyonda
    // otomatik olarak simülasyona dahil olur - ekstra bir işlem
    // gerekmez (bkz. sensors modülü, CoreHomeRegistry.refresh()).
    @Transactional
    public RegisteredApplianceResponse addAppliance(
            UUID homeId,
            UUID ownerId,
            RegisterApplianceRequest request
    ) {
        Home home = homeRepository
                .findByIdAndOwnerId(homeId, ownerId)
                .orElseThrow(() -> new HomeNotFoundException(homeId));

        Appliance appliance = new Appliance(
                UUID.randomUUID(),
                request.name().trim(),
                request.safeLimitWatt()
        );

        home.addAppliance(appliance);
        homeRepository.save(home);

        log.info(
                "Eve yeni cihaz eklendi: homeId={}, applianceId={}",
                homeId,
                appliance.getId()
        );

        return new RegisteredApplianceResponse(
                appliance.getId(),
                appliance.getName(),
                appliance.getSafeLimitWatt()
        );
    }

    @Transactional
    public void removeAppliance(UUID homeId, UUID ownerId, UUID applianceId) {
        homeRepository
                .findByIdAndOwnerId(homeId, ownerId)
                .orElseThrow(() -> new HomeNotFoundException(homeId));

        Appliance appliance = applianceRepository
                .findById(applianceId)
                .orElseThrow(() -> new ApplianceNotFoundException(applianceId));

        if (!appliance.getHome().getId().equals(homeId)) {
            throw new ApplianceNotFoundException(applianceId);
        }

        try {
            liveStateStore.removeAppliance(homeId, applianceId);
        } catch (IgniteUnavailableException exception) {
            log.warn(
                    "Cihazın Ignite canlı durumu silinemedi (yok sayıldı): applianceId={}",
                    applianceId,
                    exception
            );
        }

        applianceRepository.delete(appliance);

        log.info(
                "Cihaz silindi: homeId={}, applianceId={}",
                homeId,
                applianceId
        );
    }

    // Cihazi manuel "Durdur"/"Baslat" ile ya da bir zamanlayicinin
    // suresi dolunca durdurur/baslatir. Sadece PostgreSQL'deki bayragi
    // gunceller - fiili durma etkisi TelemetryProcessingService.process()
    // icinde uygulanir (pasifken gelen telemetri tuketime/faturaya
    // yansitilmaz, anlik guc 0 W olarak dondurulur).
    @Transactional
    public ApplianceStatusResponse setApplianceActive(
            UUID homeId,
            UUID ownerId,
            UUID applianceId,
            boolean active
    ) {
        homeRepository
                .findByIdAndOwnerId(homeId, ownerId)
                .orElseThrow(() -> new HomeNotFoundException(homeId));

        Appliance appliance = applianceRepository
                .findById(applianceId)
                .orElseThrow(() -> new ApplianceNotFoundException(applianceId));

        if (!appliance.getHome().getId().equals(homeId)) {
            throw new ApplianceNotFoundException(applianceId);
        }

        appliance.setActive(active);
        applianceRepository.save(appliance);

        log.info(
                "Cihaz durumu güncellendi: homeId={}, applianceId={}, active={}",
                homeId,
                applianceId,
                active
        );

        return new ApplianceStatusResponse(appliance.getId(), appliance.isActive());
    }
}
