package com.ondam.medication.repository;

import com.ondam.medication.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicationRepository
        extends JpaRepository<Medication, Long> {
    Optional<Medication> findByIdAndUserId(Long medicationId, Long userId); // 다른 사용자의 약을 지우는 거 막아야 되기 때문
    List<Medication> findAllByUserIdAndIsActiveTrue(Long userId);


}