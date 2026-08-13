package com.ondam.medication.repository;

import com.ondam.medication.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationRepository
        extends JpaRepository<Medication, Long> {
    List<Medication> findAllByUserIdAndIsActiveTrue(Long userId);
}