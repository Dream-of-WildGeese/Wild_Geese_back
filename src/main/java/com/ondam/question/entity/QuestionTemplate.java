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

@Entity
@Table(name = "question_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(length = 50)
    private String targetDisease;

    @Column(nullable = false)
    private boolean isActive = true;

    @Builder
    public QuestionTemplate(MetricType metricType, String content, AnswerType answerType,
                            String choices, String targetDisease) {
        this.metricType = metricType;
        this.content = content;
        this.answerType = answerType;
        this.choices = choices;
        this.targetDisease = targetDisease;
        this.isActive = true;
    }
}