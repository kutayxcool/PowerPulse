package com.powerpulse.core.advisory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RecommendationResponse(
        String title,
        UUID homeId,
        String homeName,
        List<String> recommendations,
        BigDecimal estimatedSavingPercentage,
        BigDecimal estimatedSavingAmount,
        OffsetDateTime generatedAt
) {
}