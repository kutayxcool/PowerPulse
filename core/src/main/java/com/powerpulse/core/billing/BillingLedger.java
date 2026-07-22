package com.powerpulse.core.billing;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.home.Home;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "billing_ledger")
public class BillingLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_id", nullable = false)
    private Home home;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appliance_id", nullable = false)
    private Appliance appliance;

    @Column(name = "telemetry_timestamp", nullable = false)
    private OffsetDateTime telemetryTimestamp;

    @Column(
            name = "added_consumption_kwh",
            nullable = false,
            precision = 18,
            scale = 10
    )
    private BigDecimal addedConsumptionKwh;

    @Column(
            name = "normal_charged_kwh",
            nullable = false,
            precision = 18,
            scale = 10
    )
    private BigDecimal normalChargedKwh;

    @Column(
            name = "penalty_charged_kwh",
            nullable = false,
            precision = 18,
            scale = 10
    )
    private BigDecimal penaltyChargedKwh;

    @Column(
            name = "incremental_cost",
            nullable = false,
            precision = 18,
            scale = 8
    )
    private BigDecimal incrementalCost;

    @Column(name = "penalty_tier", nullable = false)
    private int penaltyTier;

    @Column(
            name = "penalty_multiplier",
            nullable = false,
            precision = 6,
            scale = 2
    )
    private BigDecimal penaltyMultiplier;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected BillingLedger() {
    }

    public BillingLedger(
            Home home,
            Appliance appliance,
            OffsetDateTime telemetryTimestamp,
            BillingCalculation calculation
    ) {
        this.home = home;
        this.appliance = appliance;
        this.telemetryTimestamp = telemetryTimestamp;
        this.addedConsumptionKwh =
                calculation.addedConsumptionKwh();
        this.normalChargedKwh =
                calculation.normalChargedKwh();
        this.penaltyChargedKwh =
                calculation.penaltyChargedKwh();
        this.incrementalCost =
                calculation.incrementalCost();
        this.penaltyTier =
                calculation.endingPenaltyTier();
        this.penaltyMultiplier =
                calculation.endingMultiplier();
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

    public OffsetDateTime getTelemetryTimestamp() {
        return telemetryTimestamp;
    }

    public BigDecimal getAddedConsumptionKwh() {
        return addedConsumptionKwh;
    }

    public BigDecimal getNormalChargedKwh() {
        return normalChargedKwh;
    }

    public BigDecimal getPenaltyChargedKwh() {
        return penaltyChargedKwh;
    }

    public BigDecimal getIncrementalCost() {
        return incrementalCost;
    }

    public int getPenaltyTier() {
        return penaltyTier;
    }

    public BigDecimal getPenaltyMultiplier() {
        return penaltyMultiplier;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}