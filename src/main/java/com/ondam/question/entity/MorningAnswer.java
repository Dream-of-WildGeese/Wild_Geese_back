package com.ondam.question.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "morning_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MorningAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long morningQuestionId;

    @Column(nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String textValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InputType inputType;

    @Column(length = 500)
    private String audioUrl;

    @Column(nullable = false)
    private LocalDateTime answeredAt;

    @Builder
    public MorningAnswer(Long morningQuestionId, Long userId, String textValue,
                         InputType inputType, LocalDateTime answeredAt, String audioUrl) {
        this.morningQuestionId = morningQuestionId;
        this.userId = userId;
        this.textValue = textValue;
        this.inputType = inputType;
        this.answeredAt = answeredAt;
        this.audioUrl = audioUrl;
    }

    public void update(String textValue, InputType inputType) {
        this.textValue = textValue;
        this.inputType = inputType;
        this.answeredAt = LocalDateTime.now();
    }

    public void update(String textValue, InputType inputType, String audioUrl) {
        this.textValue = textValue;
        this.inputType = inputType;
        this.answeredAt = LocalDateTime.now();
        this.audioUrl = audioUrl;
    }
}