package com.ondam.checkup.repository;

import com.ondam.checkup.entity.DoctorQuestionCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorQuestionCacheRepository extends JpaRepository<DoctorQuestionCache, Long> {
    Optional<DoctorQuestionCache> findByUserId(Long userId);
}