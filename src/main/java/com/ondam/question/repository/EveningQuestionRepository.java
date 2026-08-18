package com.ondam.question.repository;

import com.ondam.question.entity.EveningQuestion;
import com.ondam.question.entity.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EveningQuestionRepository extends JpaRepository<EveningQuestion, Long> {

    List<EveningQuestion> findByUserIdAndQuestionDate(Long userId, LocalDate questionDate);
    List<EveningQuestion> findByUserIdAndMetricTypeAndQuestionDateBetween(
            Long userId, MetricType metricType, LocalDate from, LocalDate to);
    Optional<EveningQuestion> findFirstByUserIdAndMetricTypeAndQuestionDateBeforeOrderByQuestionDateDesc(
            Long userId, MetricType metricType, LocalDate questionDate);
}