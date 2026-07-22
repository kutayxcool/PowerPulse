package com.powerpulse.core.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "powerpulse.mail.enabled",
        havingValue = "true"
)
public class NotificationEmailDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationEmailDispatcher.class);

    private final NotificationLogRepository notificationRepository;
    private final EmailNotificationSender emailSender;
    private final NotificationStatusService statusService;

    public NotificationEmailDispatcher(
            NotificationLogRepository notificationRepository,
            EmailNotificationSender emailSender,
            NotificationStatusService statusService
    ) {
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
        this.statusService = statusService;
    }

    @Scheduled(
            fixedDelayString =
                    "${powerpulse.mail.dispatch-interval-ms:30000}",
            initialDelayString =
                    "${powerpulse.mail.initial-delay-ms:20000}"
    )
    public void dispatchPendingEmails() {
        var notifications =
                notificationRepository
                        .findTop50ByDeliveryStatusOrderByCreatedAtAsc(
                                NotificationDeliveryStatus.PENDING
                        );

        if (notifications.isEmpty()) {
            return;
        }

        log.info(
                "Bekleyen e-posta bildirimleri gönderiliyor. adet={}",
                notifications.size()
        );

        for (NotificationLog notification : notifications) {
            try {
                emailSender.send(notification);
                statusService.markSent(notification.getId());

                log.info(
                        "E-posta bildirimi gönderildi. notificationId={}",
                        notification.getId()
                );
            } catch (RuntimeException exception) {
                statusService.markFailed(
                        notification.getId(),
                        exception.getMessage()
                );

                log.error(
                        "E-posta bildirimi gönderilemedi. notificationId={}",
                        notification.getId(),
                        exception
                );
            }
        }
    }
}