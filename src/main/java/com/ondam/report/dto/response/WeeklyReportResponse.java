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
        String weeklyDetail,
        Map<String, MetricDetail> metrics,
        MedicationSummary medication,
        String customComment,
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

    @Builder
    public record MedicationSummary(   // ← 추가
           long takenCount,
           long totalCount,
           String comment
    ) {}
}