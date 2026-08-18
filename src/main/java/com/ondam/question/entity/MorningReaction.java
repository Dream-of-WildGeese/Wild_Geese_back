// src/main/java/com/ondam/question/entity/MorningReaction.java
package com.ondam.question.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "morning_reaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MorningReaction extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long morningAnswerId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String emoji;

    @Builder
    public MorningReaction(Long morningAnswerId, Long userId, String emoji) {
        this.morningAnswerId = morningAnswerId;
        this.userId = userId;
        this.emoji = emoji;
    }
}