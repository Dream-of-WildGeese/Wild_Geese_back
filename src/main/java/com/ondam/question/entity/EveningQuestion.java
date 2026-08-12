package com.ondam.question.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "evening_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EveningQuestion extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long templateId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate questionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MetricType metricType;

    @Column(nullable = false, length = 255)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnswerType answerType;

    @Column(columnDefinition = "json")
    private String choices;

    @Builder
    public EveningQuestion(Long templateId, Long userId, LocalDate questionDate,
                           MetricType metricType, String content,
                           AnswerType answerType, String choices) {
        this.templateId = templateId;
        this.userId = userId;
        this.questionDate = questionDate;
        this.metricType = metricType;
        this.content = content;
        this.answerType = answerType;
        this.choices = choices;
    }
}
