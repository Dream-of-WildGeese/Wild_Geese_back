package com.ondam.question.dto.response;

import com.ondam.question.entity.MetricType;
import com.ondam.question.entity.AnswerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record EveningQuestionResponse(
        String questionDate,
        int completedCount,
        int totalCount,
        List<QuestionItem> questions
) {
    @Builder
    public record QuestionItem(       // ← 여기, EveningQuestionResponse 안쪽에 있음
              Long questionId,
              MetricType metricType,
              String content,
              AnswerType answerType,
              List<ChoiceItem> choices,
              Object myAnswer
    ) {}

    @Builder
    public record ChoiceItem(String label, Double value) {}   // ← 이것도 안쪽에
}