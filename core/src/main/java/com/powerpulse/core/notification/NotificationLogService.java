package com.powerpulse.core.notification;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.event.OperationalEventType;
import com.powerpulse.core.home.Home;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationLogService {

    private final NotificationLogRepository notificationRepository;

    public NotificationLogService(
            NotificationLogRepository notificationRepository
    ) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void createPendingEmail(
            Home home,
            Appliance appliance,
            OperationalEventType eventType,
            String message
    ) {
        NotificationType notificationType =
                NotificationType.valueOf(eventType.name());

        NotificationLog notification = new NotificationLog(
                home,
                appliance,
                notificationType,
                home.getContactEmail(),
                determineSubject(notificationType),
                message
        );

        notificationRepository.save(notification);
    }

    private String determineSubject(NotificationType type) {
        return switch (type) {
            case QUOTA_WARNING_80 ->
                    "PowerPulse - Enerji kotası yüzde 80 seviyesinde";

            case QUOTA_REACHED_100 ->
                    "PowerPulse - Enerji kotası aşıldı";

            case PENALTY_TIER_CHANGED ->
                    "PowerPulse - Ceza tarifesi değişti";

            case APPLIANCE_ANOMALY ->
                    "PowerPulse - Cihaz anomalisi tespit edildi";

            case AI_RECOMMENDATION ->
                    "PowerPulse - Yeni enerji tasarrufu önerisi";
        };
    }
}