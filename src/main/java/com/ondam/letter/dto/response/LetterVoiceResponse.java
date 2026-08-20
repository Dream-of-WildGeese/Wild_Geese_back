package com.ondam.letter.dto.response;

public record LetterVoiceResponse(
        String audioUrl,
        String text
) {}