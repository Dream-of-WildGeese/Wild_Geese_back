package com.ondam.question.repository;

import com.ondam.question.entity.EveningAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EveningAnswerRepository extends JpaRepository<EveningAnswer, Long> {

    Optional<EveningAnswer> findFirstByEveningQuestionIdAndUserId(Long questionId, Long userId);
}