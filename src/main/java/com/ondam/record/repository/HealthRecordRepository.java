package com.ondam.record.repository;

import com.ondam.question.entity.MetricType;
import com.ondam.record.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    @Query("SELECT AVG(h.numericValue) FROM HealthRecord h " +
            "WHERE h.userId = :userId AND h.metricType = :metricType " +
            "AND h.recordDate BETWEEN :from AND :to")
    BigDecimal findAverageValue(
            @Param("userId") Long userId,
            @Param("metricType") MetricType metricType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    List<HealthRecord> findByUserIdAndMetricTypeAndRecordDateBetween(Long userId, MetricType metricType, LocalDate from, LocalDate to);
    Optional<HealthRecord> findByEveningAnswerId(Long eveningAnswerId);
}