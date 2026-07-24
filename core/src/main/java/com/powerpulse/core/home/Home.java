package com.powerpulse.core.home;

import com.powerpulse.core.appliance.Appliance;
import com.powerpulse.core.auth.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "homes")
public class Home {

    @Id
    private UUID id;

    // Ev artik bir kullaniciya ait - "herkes kayit olabilecek ve
    // kendi evlerini yonetebilecek" coklu kullanici sistemi. Tum
    // sorgular (bkz. HomeRepository) sahiplige gore filtrelenir ki
    // bir kullanici baskasinin evini goremesin/degistiremesin.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "budget_quota_kwh", nullable = false, precision = 12, scale = 3)
    private BigDecimal budgetQuotaKwh;

    @Column(name = "base_rate_per_kwh", nullable = false, precision = 10, scale = 4)
    private BigDecimal baseRatePerKwh;

    @Column(name = "total_consumption_kwh", nullable = false, precision = 14, scale = 4)
    private BigDecimal totalConsumptionKwh;

    @Column(name = "current_bill_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal currentBillAmount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(
            mappedBy = "home",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Appliance> appliances = new ArrayList<>();

    protected Home() {
    }

    public Home(
            UUID id,
            String name,
            String contactEmail,
            BigDecimal budgetQuotaKwh,
            BigDecimal baseRatePerKwh,
            User owner
    ) {
        this.id = id;
        this.name = name;
        this.contactEmail = contactEmail;
        this.budgetQuotaKwh = budgetQuotaKwh;
        this.baseRatePerKwh = baseRatePerKwh;
        this.owner = owner;
        this.totalConsumptionKwh = BigDecimal.ZERO;
        this.currentBillAmount = BigDecimal.ZERO;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void updateTimestamp() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void addAppliance(Appliance appliance) {
        appliances.add(appliance);
        appliance.assignToHome(this);
    }

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public BigDecimal getBudgetQuotaKwh() {
        return budgetQuotaKwh;
    }

    public BigDecimal getBaseRatePerKwh() {
        return baseRatePerKwh;
    }

    public BigDecimal getTotalConsumptionKwh() {
        return totalConsumptionKwh;
    }

    public BigDecimal getCurrentBillAmount() {
        return currentBillAmount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Appliance> getAppliances() {
        return List.copyOf(appliances);
    }

    public void updateFinancialTotals(
            BigDecimal totalConsumptionKwh,
            BigDecimal currentBillAmount
    ) {
        this.totalConsumptionKwh = totalConsumptionKwh;
        this.currentBillAmount = currentBillAmount;
    }
}