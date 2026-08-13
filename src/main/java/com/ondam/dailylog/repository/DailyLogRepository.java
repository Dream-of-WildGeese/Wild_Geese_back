package com.ondam.dailylog.repository;

import com.ondam.dailylog.entity.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {
    // 1. 특정 사용자의 특정 날짜 일지 하나 조회
    Optional<DailyLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);

    // 2. 특정 사용자의 날짜 범위 조회
    List<DailyLog> findByUserIdAndLogDateBetween(Long userId, LocalDate from, LocalDate to);
}
