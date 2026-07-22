package com.powerpulse.core.notification;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.home.Home;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_id", nullable = false)
    private Home home;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appliance_id")
    private Appliance appliance;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private NotificationDeliveryStatus deliveryStatus;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    protected NotificationLog() {
    }

    public NotificationLog(
            Home home,
            Appliance appliance,
            NotificationType notificationType,
            String recipientEmail,
            String subject,
            String message
    ) {
        this.home = home;
        this.appliance = appliance;
        this.notificationType = notificationType;
        this.channel = NotificationChannel.EMAIL;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.message = message;
        this.deliveryStatus = NotificationDeliveryStatus.PENDING;
        this.createdAt = OffsetDateTime.now();
    }

    public void markSent() {
        this.deliveryStatus = NotificationDeliveryStatus.SENT;
        this.sentAt = OffsetDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.deliveryStatus = NotificationDeliveryStatus.FAILED;
        this.errorMessage = errorMessage;
        this.sentAt = null;
    }

    public Long getId() {
        return id;
    }

    public Home getHome() {
        return home;
    }

    public Appliance getAppliance() {
        return appliance;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public NotificationDeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }
}