package com.ondam.medication.repository;

import com.ondam.medication.entity.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationScheduleRepository
        extends JpaRepository<MedicationSchedule, Long> {
    List<MedicationSchedule> findAllByMedicationId(Long medicationId);
}