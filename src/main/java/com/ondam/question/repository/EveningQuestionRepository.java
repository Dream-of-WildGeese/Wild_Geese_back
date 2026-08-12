package com.ondam.question.repository;

import com.ondam.question.entity.EveningQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EveningQuestionRepository extends JpaRepository<EveningQuestion, Long> {

    List<EveningQuestion> findByUserIdAndQuestionDate(Long userId, LocalDate questionDate);
}