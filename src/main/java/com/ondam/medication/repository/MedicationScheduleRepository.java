package com.ondam.medication.repository;

import com.ondam.medication.entity.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationScheduleRepository
        extends JpaRepository<MedicationSchedule, Long> {
}