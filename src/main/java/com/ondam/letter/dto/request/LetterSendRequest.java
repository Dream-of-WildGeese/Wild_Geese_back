package com.ondam.letter.dto.request;

import com.ondam.question.entity.InputType;

public record LetterSendRequest(
        Long toUserId,
        String content,
        InputType inputType,
        String audioUrl // 👈 오디오 경로 필드 추가!
) {}