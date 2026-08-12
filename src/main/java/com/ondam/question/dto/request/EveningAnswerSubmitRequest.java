package com.ondam.question.dto.request;

import com.ondam.question.entity.InputType;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EveningAnswerSubmitRequest {

    private List<AnswerItem> answers;

    @Getter
    @NoArgsConstructor
    public static class AnswerItem {
        private Long questionId;
        private String textValue;
        private String choiceValue;
        private InputType inputType;
    }
}
