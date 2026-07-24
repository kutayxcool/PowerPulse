package com.powerpulse.core.event;

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
@Table(name = "operational_events")
public class OperationalEvent {

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
    @Column(name = "event_type", nullable = false, length = 50)
    private OperationalEventType eventType;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "event_time", nullable = false)
    private OffsetDateTime eventTime;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected OperationalEvent() {
    }

    public OperationalEvent(
            Home home,
            Appliance appliance,
            OperationalEventType eventType,
            String message,
            OffsetDateTime eventTime
    ) {
        this.home = home;
        this.appliance = appliance;
        this.eventType = eventType;
        this.message = message;
        this.eventTime = eventTime;
        this.createdAt = OffsetDateTime.now();
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

    public OperationalEventType getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public OffsetDateTime getEventTime() {
        return eventTime;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}