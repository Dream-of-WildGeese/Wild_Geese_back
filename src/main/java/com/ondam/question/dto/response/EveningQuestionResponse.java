package com.ondam.question.dto.response;

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