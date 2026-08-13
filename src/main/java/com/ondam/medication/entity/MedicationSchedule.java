package com.ondam.medication.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "medication_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "medication_schedule_days",
            joinColumns = @JoinColumn(name = "schedule_id")
    )
    @Column(name = "day_of_week", nullable = false)
    private Set<MedicationDay> daysOfWeek = new HashSet<>();

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;

    public MedicationSchedule(
            Medication medication,
            LocalTime scheduledTime,
            Set<MedicationDay> daysOfWeek
    ) {
        this.medication = medication;
        this.scheduledTime = scheduledTime;
        this.daysOfWeek = daysOfWeek;
        this.isEnabled = true;
    }
}