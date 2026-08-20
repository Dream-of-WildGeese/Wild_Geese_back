package com.ondam.question.repository;

import com.ondam.question.entity.MorningReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface MorningReactionRepository extends JpaRepository<MorningReaction, Long> {
    Optional<MorningReaction> findByMorningAnswerIdAndUserIdAndEmoji(Long answerId, Long userId, String emoji);
    List<MorningReaction> findByMorningAnswerId(Long answerId);
}