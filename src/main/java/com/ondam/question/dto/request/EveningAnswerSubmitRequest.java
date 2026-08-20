package com.ondam.question.dto.request;

import com.ondam.question.entity.InputType;

import java.util.List;

public record EveningAnswerSubmitRequest(
        List<AnswerItem> answers
) {
    public record AnswerItem(
            Long questionId,
            String textValue,
            String choiceValue,
            InputType inputType
    ) {}
}