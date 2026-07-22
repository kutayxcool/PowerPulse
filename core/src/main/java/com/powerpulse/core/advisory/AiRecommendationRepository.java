package com.powerpulse.core.advisory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRecommendationRepository
        extends JpaRepository<AiRecommendation, Long> {
}