package com.ondam.record.entity;

import com.ondam.global.common.BaseTimeEntity;
import com.ondam.question.entity.MetricType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "health_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MetricType metricType;

    @Column(precision = 8, scale = 2)
    private BigDecimal numericValue;

    @Column(length = 255)
    private String textValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceType source;

    private Long eveningAnswerId;

    @Builder
    public HealthRecord(Long userId, LocalDate recordDate, MetricType metricType,
                        BigDecimal numericValue, String textValue, SourceType source,
                        Long eveningAnswerId) {
        this.userId = userId;
        this.recordDate = recordDate;
        this.metricType = metricType;
        this.numericValue = numericValue;
        this.textValue = textValue;
        this.source = source;
        this.eveningAnswerId = eveningAnswerId;
    }
}
