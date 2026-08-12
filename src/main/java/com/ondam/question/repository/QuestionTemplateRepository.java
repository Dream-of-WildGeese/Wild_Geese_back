package com.ondam.question.repository;

import com.ondam.question.entity.MetricType;
import com.ondam.question.entity.QuestionTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, Long> {
    List<QuestionTemplate> findByMetricType(MetricType metricType);
}
