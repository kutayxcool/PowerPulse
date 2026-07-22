package com.powerpulse.core.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationStatusService {

    private final NotificationLogRepository notificationRepository;

    public NotificationStatusService(
            NotificationLogRepository notificationRepository
    ) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void markSent(Long notificationId) {
        notificationRepository
                .findById(notificationId)
                .ifPresent(NotificationLog::markSent);
    }

    @Transactional
    public void markFailed(
            Long notificationId,
            String errorMessage
    ) {
        notificationRepository
                .findById(notificationId)
                .ifPresent(notification ->
                        notification.markFailed(
                                limitErrorMessage(errorMessage)
                        )
                );
    }

    private String limitErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Bilinmeyen e-posta gönderim hatası.";
        }

        if (errorMessage.length() <= 1000) {
            return errorMessage;
        }

        return errorMessage.substring(0, 1000);
    }
}