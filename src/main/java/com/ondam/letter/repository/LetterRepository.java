package com.ondam.letter.repository;

import com.ondam.letter.entity.Letter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LetterRepository extends JpaRepository<Letter, Long> {

    // 1. toUserId로 받은 편지 목록 조회 (페이징)
    Page<Letter> findByToUserId(Long toUserId, Pageable pageable);

    // 2. fromUserId로 보낸 편지 목록 조회 (페이징)
    Page<Letter> findByFromUserId(Long fromUserId, Pageable pageable);
}