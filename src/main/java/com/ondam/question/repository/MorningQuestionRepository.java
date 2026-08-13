package com.ondam.question.repository;

import com.ondam.question.entity.MorningQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MorningQuestionRepository extends JpaRepository<MorningQuestion, Long>{
    Optional<MorningQuestion> findByFamilyIdAndQuestionDate(Long familyId, LocalDate questionDate);
}
