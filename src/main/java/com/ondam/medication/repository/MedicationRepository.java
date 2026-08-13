package com.ondam.medication.repository;

import com.ondam.medication.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationRepository
        extends JpaRepository<Medication, Long> {
}