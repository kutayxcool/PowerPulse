package com.powerpulse.core.snapshot;

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
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "daily_consumption_snapshots")
public class DailyConsumptionSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_id", nullable = false)
    private Home home;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(
            name = "consumption_kwh",
            nullable = false,
            precision = 14,
            scale = 4
    )
    private BigDecimal consumptionKwh;

    @Column(
            name = "bill_amount",
            nullable = false,
            precision = 14,
            scale = 2
    )
    private BigDecimal billAmount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected DailyConsumptionSnapshot() {
    }

    public DailyConsumptionSnapshot(
            Home home,
            LocalDate snapshotDate,
            BigDecimal consumptionKwh,
            BigDecimal billAmount
    ) {
        this.home = home;
        this.snapshotDate = snapshotDate;
        this.consumptionKwh = consumptionKwh;
        this.billAmount = billAmount;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Home getHome() {
        return home;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public BigDecimal getConsumptionKwh() {
        return consumptionKwh;
    }

    public BigDecimal getBillAmount() {
        return billAmount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    public void updateValues(
            BigDecimal consumptionKwh,
            BigDecimal billAmount
    ) {
        this.consumptionKwh = consumptionKwh;
        this.billAmount = billAmount;
        this.createdAt = OffsetDateTime.now();
    }
}