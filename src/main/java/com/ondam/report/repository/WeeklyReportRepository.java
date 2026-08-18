package com.ondam.report.repository;

import com.ondam.report.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    Optional<WeeklyReport> findByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);
    List<WeeklyReport> findByUserIdOrderByWeekStartDateDesc(Long userId);
}