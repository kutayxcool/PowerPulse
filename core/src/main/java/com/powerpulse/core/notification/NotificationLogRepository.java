package com.powerpulse.core.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository
        extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog>
    findTop50ByDeliveryStatusOrderByCreatedAtAsc(
            NotificationDeliveryStatus deliveryStatus
    );
}