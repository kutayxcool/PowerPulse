package com.powerpulse.core.appliance;

import com.powerpulse.core.home.Home;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "appliances")
public class Appliance {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_id", nullable = false)
    private Home home;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "safe_limit_watt", nullable = false, precision = 12, scale = 2)
    private BigDecimal safeLimitWatt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Appliance() {
    }

    public Appliance(UUID id, String name, BigDecimal safeLimitWatt) {
        this.id = id;
        this.name = name;
        this.safeLimitWatt = safeLimitWatt;
        this.createdAt = OffsetDateTime.now();
    }

    public void assignToHome(Home home) {
        this.home = home;
    }

    public UUID getId() {
        return id;
    }

    public Home getHome() {
        return home;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getSafeLimitWatt() {
        return safeLimitWatt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}