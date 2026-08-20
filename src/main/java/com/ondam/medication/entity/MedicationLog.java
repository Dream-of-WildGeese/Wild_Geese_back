package com.ondam.medication.entity;

import com.ondam.global.common.BaseTimeEntity;
import com.ondam.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "medication_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_medication_log_schedule_date",
                        columnNames = {"schedule_id", "record_date"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private MedicationSchedule schedule;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @Column(name = "is_retroactive", nullable = false)
    private boolean isRetroactive = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MedicationLogStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public MedicationLog(
            MedicationSchedule schedule,
            User user,
            LocalDate recordDate,
            MedicationLogStatus status,
            LocalDateTime checkedAt,
            boolean isRetroactive
    ) {
        this.schedule = schedule;
        this.user = user;
        this.recordDate = recordDate;
        this.status = status;
        this.checkedAt = checkedAt;
        this.isRetroactive = isRetroactive;
    }

    public void updateStatus(
            MedicationLogStatus status,
            LocalDateTime checkedAt,
            boolean isRetroactive
    ) {
        this.status = status;
        this.checkedAt = checkedAt;
        this.isRetroactive = isRetroactive;
    }

}