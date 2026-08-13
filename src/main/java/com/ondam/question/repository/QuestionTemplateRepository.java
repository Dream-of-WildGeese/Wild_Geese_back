package com.ondam.question.repository;

import com.ondam.question.entity.MetricType;
import com.ondam.question.entity.QuestionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, Long> {
    List<QuestionTemplate> findByMetricTypeAndIsActiveTrue(MetricType metricType);
    List<QuestionTemplate> findByMetricTypeAndTargetDiseaseAndIsActiveTrue(MetricType metricType, String targetDisease);
}