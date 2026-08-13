package com.ondam.report.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "weekly_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate weekStartDate;

    @Column(nullable = false)
    private LocalDate weekEndDate;

    @Column(columnDefinition = "json")
    private String metricsSummary;

    @Column(columnDefinition = "text")
    private String aiSummary;

    @Column(nullable = false)
    private boolean isBaselineSufficient;

    @Builder
    public WeeklyReport(Long userId, LocalDate weekStartDate, LocalDate weekEndDate,
                        String metricsSummary, String aiSummary, boolean isBaselineSufficient){
        this.userId = userId;
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
        this.metricsSummary = metricsSummary;
        this.aiSummary = aiSummary;
        this.isBaselineSufficient = isBaselineSufficient;
    }
}
