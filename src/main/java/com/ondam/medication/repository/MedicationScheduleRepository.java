package com.ondam.medication.repository;

import com.ondam.medication.entity.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationScheduleRepository
        extends JpaRepository<MedicationSchedule, Long> {
    List<MedicationSchedule> findAllByMedicationId(Long medicationId);
    void deleteAllByMedicationId(Long medicationId); // 기존 스케줄을 모두 지우고 새로 만드는 방식
    List<MedicationSchedule> findAllByMedicationUserIdAndMedicationIsActiveTrueAndIsEnabledTrue(
            Long userId
    );
}