package com.ondam.question.dto.response;

public record VoiceTranscribeResponse(
        String transcript,
        String matchedChoice
) {}