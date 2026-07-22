package com.powerpulse.core.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerpulse.core.ignite.IgniteCacheNames;
import com.powerpulse.core.ignite.IgniteCacheService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class LiveStateStore {

    private final IgniteCacheService cacheService;
    private final ObjectMapper objectMapper;

    public LiveStateStore(
            IgniteCacheService cacheService,
            ObjectMapper objectMapper
    ) {
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    public Optional<HomeLiveState> findHome(UUID homeId) {
        return cacheService
                .getJson(
                        IgniteCacheNames.HOME_LIVE_STATE,
                        homeId.toString()
                )
                .map(json -> readJson(json, HomeLiveState.class));
    }

    public void saveHome(HomeLiveState state) {
        cacheService.putJson(
                IgniteCacheNames.HOME_LIVE_STATE,
                state.homeId().toString(),
                writeJson(state)
        );
    }

    public Optional<ApplianceLiveState> findAppliance(
            UUID homeId,
            UUID applianceId
    ) {
        return cacheService
                .getJson(
                        IgniteCacheNames.APPLIANCE_LIVE_STATE,
                        applianceKey(homeId, applianceId)
                )
                .map(json -> readJson(json, ApplianceLiveState.class));
    }

    public void saveAppliance(ApplianceLiveState state) {
        cacheService.putJson(
                IgniteCacheNames.APPLIANCE_LIVE_STATE,
                applianceKey(state.homeId(), state.applianceId()),
                writeJson(state)
        );
    }

    private String applianceKey(UUID homeId, UUID applianceId) {
        return homeId + ":" + applianceId;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new LiveStateSerializationException(
                    "Canlı durum JSON formatına dönüştürülemedi.",
                    exception
            );
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new LiveStateSerializationException(
                    "Ignite canlı durum verisi okunamadı.",
                    exception
            );
        }
    }
}