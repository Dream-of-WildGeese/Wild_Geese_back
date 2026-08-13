package com.ondam.medication.repository;

import com.ondam.medication.entity.MedicationLog;
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
}