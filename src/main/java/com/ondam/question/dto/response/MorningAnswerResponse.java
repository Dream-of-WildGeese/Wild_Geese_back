package com.ondam.question.dto.response;

public record MorningAnswerResponse(
        Long answerId,
        String audioUrl,
        String textValue
) {}