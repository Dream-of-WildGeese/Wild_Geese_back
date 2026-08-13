package com.ondam.report.dto.response;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record WeeklyReportResponse(
        String weekStartDate,
        String weekEndDate,
        boolean isBaselineSufficient,
        String weeklyComment,
        Map<String, MetricDetail> metrics,
        String nextWeekSuggestion
) {
    @Builder
    public record MetricDetail(
            Double current,
            Double previous,
            Double diff,
            String trend,
            List<Double> daily,
            String comment
    ) {}
}