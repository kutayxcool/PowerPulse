package com.powerpulse.core.event;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.home.Home;
import com.powerpulse.core.notification.NotificationLogService;
import com.powerpulse.core.telemetry.HomeLiveState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class OperationalEventService {

    private final OperationalEventRepository eventRepository;
    private final NotificationLogService notificationLogService;

    public OperationalEventService(
            OperationalEventRepository eventRepository,
            NotificationLogService notificationLogService
    ) {
        this.eventRepository = eventRepository;
        this.notificationLogService = notificationLogService;
    }

    @Transactional
    public void recordTransitions(
            Home home,
            Appliance appliance,
            HomeLiveState previousState,
            HomeLiveState newState,
            int previousBreachCount,
            int newBreachCount,
            OffsetDateTime eventTime
    ) {
        recordQuotaEvents(
                home,
                previousState,
                newState,
                eventTime
        );

        recordPenaltyEvent(
                home,
                previousState,
                newState,
                eventTime
        );

        recordAnomalyEvent(
                home,
                appliance,
                previousBreachCount,
                newBreachCount,
                eventTime
        );
    }

    private void recordQuotaEvents(
            Home home,
            HomeLiveState previousState,
            HomeLiveState newState,
            OffsetDateTime eventTime
    ) {
        double previousPercentage =
                previousState.quotaPercentage();

        double newPercentage =
                newState.quotaPercentage();

        if (previousPercentage < 80.0 && newPercentage >= 80.0) {
            save(
                    home,
                    null,
                    OperationalEventType.QUOTA_WARNING_80,
                    "Ev enerji kotasının yüzde 80 seviyesine ulaştı.",
                    eventTime
            );
        }

        if (previousPercentage < 100.0 && newPercentage >= 100.0) {
            save(
                    home,
                    null,
                    OperationalEventType.QUOTA_REACHED_100,
                    "Ev enerji kotasının yüzde 100 seviyesine ulaştı.",
                    eventTime
            );
        }
    }

    private void recordPenaltyEvent(
            Home home,
            HomeLiveState previousState,
            HomeLiveState newState,
            OffsetDateTime eventTime
    ) {
        if (previousState.penaltyTier() == newState.penaltyTier()) {
            return;
        }

        save(
                home,
                null,
                OperationalEventType.PENALTY_TIER_CHANGED,
                "Ceza tarifesi kademesi "
                        + previousState.penaltyTier()
                        + " seviyesinden "
                        + newState.penaltyTier()
                        + " seviyesine değişti.",
                eventTime
        );
    }

    private void recordAnomalyEvent(
            Home home,
            Appliance appliance,
            int previousBreachCount,
            int newBreachCount,
            OffsetDateTime eventTime
    ) {
        if (previousBreachCount < 3 && newBreachCount >= 3) {
            save(
                    home,
                    appliance,
                    OperationalEventType.APPLIANCE_ANOMALY,
                    appliance.getName()
                            + " güvenli güç limitini art arda üç kez aştı.",
                    eventTime
            );
        }
    }

    private void save(
            Home home,
            Appliance appliance,
            OperationalEventType eventType,
            String message,
            OffsetDateTime eventTime
    ) {
        eventRepository.save(
                new OperationalEvent(
                        home,
                        appliance,
                        eventType,
                        message,
                        eventTime
                )
        );

        notificationLogService.createPendingEmail(
                home,
                appliance,
                eventType,
                message
        );
    }
}