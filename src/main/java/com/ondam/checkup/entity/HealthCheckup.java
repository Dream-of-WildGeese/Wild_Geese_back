package com.ondam.checkup.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "health_checkup")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthCheckup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate checkupDate;

    @Column(nullable = false, length = 100)
    private String checkupType; // 예: "일반검진 (국가건강검진)", "위내시경"

    @Column(length = 100)
    private String hospitalName; // 예: "온담 종합병원"

    @Column(nullable = false)
    private boolean isCompleted;

    @Builder
    public HealthCheckup(Long userId, LocalDate checkupDate, String checkupType, String hospitalName, boolean isCompleted) {
        this.userId = userId;
        this.checkupDate = checkupDate;
        this.checkupType = checkupType;
        this.hospitalName = hospitalName;
        this.isCompleted = isCompleted;
    }

    public void update(LocalDate checkupDate, String checkupType, String hospitalName, Boolean isCompleted) {
        if (checkupDate != null) this.checkupDate = checkupDate;
        if (checkupType != null) this.checkupType = checkupType;
        if (hospitalName != null) this.hospitalName = hospitalName;
        if (isCompleted != null) this.isCompleted = isCompleted;
    }

    public void complete() {
        this.isCompleted = true;
    }
}