package com.ondam.question.dto.response;

import lombok.Builder;

@Builder
public record MorningQuestionHistoryItem(
        Long questionId,
        String questionDate,
        String content
) {}