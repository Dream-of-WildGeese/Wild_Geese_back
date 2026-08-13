package com.ondam.medication.repository;

import com.ondam.medication.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicationRepository
        extends JpaRepository<Medication, Long> {
    Optional<Medication> findByIdAndUserId(Long medicationId, Long userId);
    List<Medication> findAllByUserIdAndIsActiveTrue(Long userId);
}