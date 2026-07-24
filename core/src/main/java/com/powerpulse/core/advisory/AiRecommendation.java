package com.powerpulse.core.advisory;

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
@Table(name = "ai_recommendations")
public class AiRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_id", nullable = false)
    private Home home;

    @Column(nullable = false)
    private String title;

    @Column(name = "recommendation_text", nullable = false)
    private String recommendationText;

    @Column(
            name = "estimated_saving_percentage",
            precision = 5,
            scale = 2
    )
    private BigDecimal estimatedSavingPercentage;

    @Column(
            name = "estimated_saving_amount",
            precision = 14,
            scale = 2
    )
    private BigDecimal estimatedSavingAmount;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    protected AiRecommendation() {
    }

    public AiRecommendation(
            Home home,
            String title,
            String recommendationText,
            BigDecimal estimatedSavingPercentage,
            BigDecimal estimatedSavingAmount,
            OffsetDateTime generatedAt
    ) {
        this.home = home;
        this.title = title;
        this.recommendationText = recommendationText;
        this.estimatedSavingPercentage =
                estimatedSavingPercentage;
        this.estimatedSavingAmount =
                estimatedSavingAmount;
        this.generatedAt = generatedAt;
    }

    public Long getId() {
        return id;
    }

    public Home getHome() {
        return home;
    }

    public String getTitle() {
        return title;
    }

    public String getRecommendationText() {
        return recommendationText;
    }

    public BigDecimal getEstimatedSavingPercentage() {
        return estimatedSavingPercentage;
    }

    public BigDecimal getEstimatedSavingAmount() {
        return estimatedSavingAmount;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }
}