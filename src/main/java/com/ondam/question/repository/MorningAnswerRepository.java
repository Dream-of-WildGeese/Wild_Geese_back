package com.ondam.question.repository;

import com.ondam.question.entity.MorningAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface MorningAnswerRepository extends JpaRepository<MorningAnswer, Long>{

    List<MorningAnswer> findByMorningQuestionId(Long morningQuestionId);
    Optional<MorningAnswer> findByMorningQuestionIdAndUserId(Long morningQuestionId, Long userId);
}
