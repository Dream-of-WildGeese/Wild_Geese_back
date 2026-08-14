package com.ondam.letter.dto.request;

import com.ondam.question.entity.InputType;

public record LetterSendRequest(
        Long toUserId,
        String content,
        InputType inputType
) {}