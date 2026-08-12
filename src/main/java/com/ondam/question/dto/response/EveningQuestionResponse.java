package com.ondam.question.dto.response;

import com.ondam.question.entity.AnswerType;
import com.ondam.question.entity.MetricType;

import java.util.List;

public record EveningQuestionResponse(
        String questionDate,
        int completedCount,
        int totalCount,
        List<QuestionItem> questions
) {
    public record QuestionItem(       // ← 여기, EveningQuestionResponse 안쪽에 있음
              Long questionId,
              MetricType metricType,
              String content,
              AnswerType answerType,
              List<ChoiceItem> choices,
              Object myAnswer
    ) {}

    public record ChoiceItem(String label, Double value) {}   // ← 이것도 안쪽에
}