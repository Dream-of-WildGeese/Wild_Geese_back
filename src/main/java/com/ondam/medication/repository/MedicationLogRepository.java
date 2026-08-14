package com.ondam.medication.repository;

import com.ondam.medication.entity.MedicationLog;
import com.ondam.medication.entity.MedicationLogStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MedicationLogRepository
        extends JpaRepository<MedicationLog, Long> {

    Optional<MedicationLog>
    findByScheduleIdAndRecordDate(
            Long scheduleId,
            LocalDate recordDate
    );

    long countByUserIdAndStatusAndRecordDateBetween(
            Long userId,
            MedicationLogStatus status,
            LocalDate from,
            LocalDate to
    );
}