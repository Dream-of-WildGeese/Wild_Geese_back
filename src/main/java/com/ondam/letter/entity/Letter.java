package com.ondam.letter.entity;

import com.ondam.global.common.BaseTimeEntity;
import com.ondam.question.entity.InputType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "letter")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Letter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fromUserId;

    @Column(nullable = false)
    private Long toUserId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InputType inputType;

    @Column(length = 500)
    private String audioUrl;

    private LocalDateTime readAt;

    @Builder
    public Letter(Long fromUserId, Long toUserId, String content,
                  InputType inputType, String audioUrl) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.content = content;
        this.inputType = inputType;
        this.audioUrl = audioUrl;
    }

    public void markAsRead() {
        this.readAt = LocalDateTime.now();
    }
}