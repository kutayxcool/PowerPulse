package com.powerpulse.core.advisory;

import java.util.List;

public record AdvisoryResult(
        String title,
        List<String> recommendations,
        double estimatedSavingPercentage,
        double estimatedSavingAmount
) {
}