package com.ondam.user.repository;

import com.ondam.user.entity.HealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthProfileRepository
        extends JpaRepository<HealthProfile, Long> {

    Optional<HealthProfile> findByUserId(Long userId);
}