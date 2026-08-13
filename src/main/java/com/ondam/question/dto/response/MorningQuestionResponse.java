package com.ondam.question.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record MorningQuestionResponse (
    Long questionId,
    String questionDate,
    String content,
    String myAnswer,
    List<FamilyAnswerItem> familyAnswers
) {
    @Builder
    public record FamilyAnswerItem(
            Long userId,
            String name,
            String role,
            String textValue,
            LocalDateTime answeredAt
    ) {}
}
