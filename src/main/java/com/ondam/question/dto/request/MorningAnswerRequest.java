package com.ondam.question.dto.request;

import com.ondam.question.entity.InputType;

public record MorningAnswerRequest(
        String textValue,
        InputType inputType
) {}