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

import java.time.LocalDateTime;

@Entity
@Table(name = "evening_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EveningAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eveningQuestionId;

    @Column(nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String textValue;

    @Column(length = 50)
    private String choiceValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InputType inputType;

    @Column(nullable = false)
    private LocalDateTime answeredAt;

    @Builder
    public EveningAnswer(Long eveningQuestionId, Long userId, String textValue,
                         String choiceValue, InputType inputType, LocalDateTime answeredAt) {
        this.eveningQuestionId = eveningQuestionId;
        this.userId = userId;
        this.textValue = textValue;
        this.choiceValue = choiceValue;
        this.inputType = inputType;
        this.answeredAt = answeredAt;
    }
}
