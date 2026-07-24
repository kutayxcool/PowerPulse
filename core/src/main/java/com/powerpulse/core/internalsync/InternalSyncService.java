package com.powerpulse.core.internalsync;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.home.Home;
import com.powerpulse.core.home.HomeNotFoundException;
import com.powerpulse.core.home.HomeRepository;
import com.powerpulse.core.internalsync.dto.InternalApplianceResponse;
import com.powerpulse.core.internalsync.dto.InternalHomeDetailResponse;
import com.powerpulse.core.internalsync.dto.InternalHomeSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// Sensors modulu, hangi ev/cihazlar icin sahte telemetry uretecegini
// buradan ceker. Bu, uygulamadaki TEK kasitli olarak sahiplik
// (owner) filtresi UYGULANMAYAN servistir - Sensors sistemdeki TUM
// evler icin (hangi kullaniciya ait olursa olsun) telemetry uretmesi
// gerektigi icin cok kiracili (multi-tenant) bir kisitlamaya tabi
// degildir. Bu yuzden bu servise/controller'a erisim JWT yerine ayri
// bir paylasilan-sifre (shared secret) mekanizmasiyla korunur (bkz.
// InternalApiKeyFilter + SecurityConfig).
@Service
public class InternalSyncService {

    private final HomeRepository homeRepository;

    public InternalSyncService(HomeRepository homeRepository) {
        this.homeRepository = homeRepository;
    }

    @Transactional(readOnly = true)
    public List<InternalHomeSummaryResponse> getAllHomeIds() {
        return homeRepository
                .findAll()
                .stream()
                .map(home -> new InternalHomeSummaryResponse(home.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public InternalHomeDetailResponse getHomeDetail(UUID homeId) {
        Home home = homeRepository
                .findWithAppliancesById(homeId)
                .orElseThrow(() -> new HomeNotFoundException(homeId));

        List<InternalApplianceResponse> appliances = home
                .getAppliances()
                .stream()
                .map(this::toApplianceResponse)
                .toList();

        return new InternalHomeDetailResponse(
                home.getId(),
                home.getName(),
                appliances
        );
    }

    private InternalApplianceResponse toApplianceResponse(Appliance appliance) {
        return new InternalApplianceResponse(
                appliance.getId(),
                appliance.getName(),
                appliance.getSafeLimitWatt()
        );
    }
}
