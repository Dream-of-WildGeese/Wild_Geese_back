package com.ondam.user.repository;

import com.ondam.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    List<User> findAllByFamilyId(Long familyId);

    long countByFamilyId(Long familyId);

    boolean existsByInviteCode(String inviteCode);

    Optional<User> findByInviteCode(String inviteCode);

    Optional<User> findFirstByFamilyIdAndIdNot(
            Long familyId,
            Long userId
    );
}