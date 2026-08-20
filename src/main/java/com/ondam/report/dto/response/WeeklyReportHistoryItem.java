package com.ondam.report.dto.response;

import lombok.Builder;

@Builder
public record WeeklyReportHistoryItem(
        String weekStartDate,
        String weekEndDate,
        String weeklyComment
) {}