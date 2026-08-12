package com.ondam.question.dto.response;

import com.ondam.question.entity.AnswerType;
import com.ondam.question.entity.MetricType;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class EveningQuestionResponse {

    private LocalDate questionDate;
    private int completedCount;
    private int totalCount;
    private List<QuestionItem> questions;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class QuestionItem {
        private Long questionId;
        private MetricType metricType;
        private String content;
        private AnswerType answerType;
        private String choices;
        private String myAnswer;
    }
}
