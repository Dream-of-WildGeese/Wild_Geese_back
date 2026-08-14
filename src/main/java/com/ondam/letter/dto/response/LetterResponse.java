package com.ondam.letter.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LetterResponse(
        Long letterId,
        FromUserItem fromUser,
        ToUserItem toUser,
        String content,
        String inputType,
        String audioUrl,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    @Builder
    public record FromUserItem(
            Long userId,
            String name,
            String role
    ) {}

    @Builder
    public record ToUserItem(
            Long userId,
            String name,
            String role
    ) {}
}