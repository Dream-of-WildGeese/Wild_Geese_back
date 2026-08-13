package com.ondam.dailylog.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate logDate;

    @Column(columnDefinition = "json")
    private String morningAnswer;

    @Column(columnDefinition = "json")
    private String eveningAnswers;

    private Integer stepCount;

    @Column(length = 30)
    private String sleepStatus;

    @Column(columnDefinition = "json")
    private String medicationLogs;

    @Column(length = 500)
    private String summaryText;

    // 캘린더 조회용 카운트 (JSON 파싱 없이 빠르게 조회하려는 목적)
    @Column(nullable = false)
    private boolean morningAnswered;

    @Column(nullable = false)
    private int eveningCompletedCount;

    @Column(nullable = false)
    private int eveningTotalCount;

    @Builder
    public DailyLog(Long userId, LocalDate logDate, String morningAnswer, String eveningAnswers,
                    Integer stepCount, String sleepStatus, String medicationLogs, String summaryText,
                    boolean morningAnswered, int eveningCompletedCount, int eveningTotalCount) {
        this.userId = userId;
        this.logDate = logDate;
        this.morningAnswer = morningAnswer;
        this.eveningAnswers = eveningAnswers;
        this.stepCount = stepCount;
        this.sleepStatus = sleepStatus;
        this.medicationLogs = medicationLogs;
        this.summaryText = summaryText;
        this.morningAnswered = morningAnswered;
        this.eveningCompletedCount = eveningCompletedCount;
        this.eveningTotalCount = eveningTotalCount;
    }
}
