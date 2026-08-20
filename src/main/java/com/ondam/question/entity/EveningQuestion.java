package com.ondam.question.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

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
