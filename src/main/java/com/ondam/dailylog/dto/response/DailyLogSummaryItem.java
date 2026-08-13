package com.ondam.dailylog.dto.response;

import lombok.Builder;

@Builder
public record DailyLogSummaryItem (
    String logDate,
    boolean morningAnswered,
    int eveningCompletedCount,
    int eveningTotalCount
) {}