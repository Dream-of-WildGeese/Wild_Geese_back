package com.ondam.question.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "morning_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MorningQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long familyId;

    @Column(nullable = false)
    private LocalDate questionDate;

    @Column(length = 255, nullable = false)
    private String content;

    @Builder
    public MorningQuestion(Long familyId, LocalDate questionDate, String content){
        this.familyId = familyId;
        this.questionDate  = questionDate;
        this.content = content;
    }

}
