package com.ondam.checkup.repository;

import com.ondam.checkup.entity.HealthCheckup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthCheckupRepository extends JpaRepository<HealthCheckup, Long> {

    // 1. 다가오는 가장 가까운 검진 1건 (오늘 포함 미래 검진)
    Optional<HealthCheckup> findFirstByUserIdAndCheckupDateGreaterThanEqualAndIsCompletedFalseOrderByCheckupDateAsc(
            Long userId, LocalDate today);

    // 2. 지난 검진 이력 목록 (오늘 이전이거나 완료 처리된 검진)
    List<HealthCheckup> findByUserIdAndCheckupDateBeforeOrderByCheckupDateDesc(
            Long userId, LocalDate today);

    // 3. 특정 사용자의 전체 검진 일정
    List<HealthCheckup> findAllByUserIdOrderByCheckupDateDesc(Long userId);

    Optional<HealthCheckup> findByIdAndUserId(Long id, Long userId);

    @Query("""
    SELECT h
    FROM HealthCheckup h
    WHERE h.checkupDate = :checkupDate
      AND h.reminderDaysBefore = :reminderDaysBefore
      AND h.isCompleted = false
""")
    List<HealthCheckup> findCheckupsForReminder(
            @Param("checkupDate") LocalDate checkupDate,
            @Param("reminderDaysBefore") Integer reminderDaysBefore
    );
}