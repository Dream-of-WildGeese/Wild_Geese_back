package com.ondam.dailylog.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record DailyLogResponse(
        String logDate,
        boolean morningAnswered,
        int eveningCompletedCount,
        int eveningTotalCount,
        MorningAnswerItem morningAnswer,
        List<EveningAnswerItem> eveningAnswers,
        String summaryText
) {
    @Builder
    public record MorningAnswerItem(
            String textValue,
            LocalDateTime answeredAt
    ) {}

    @Builder
    public record EveningAnswerItem(
            String metricType,
            String textValue,
            String choiceValue,
            LocalDateTime answeredAt
    ) {}
}