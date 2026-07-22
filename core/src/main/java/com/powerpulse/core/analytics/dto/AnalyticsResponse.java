package com.powerpulse.core.analytics.dto;

import java.util.List;

public record AnalyticsResponse(
        List<DailyTotalConsumptionResponse> dailyTotalConsumption,
        List<HomeComparisonResponse> homeComparison
) {
}