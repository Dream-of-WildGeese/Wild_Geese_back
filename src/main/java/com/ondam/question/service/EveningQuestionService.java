package com.ondam.question.service;

import com.ondam.question.repository.EveningQuestionRepository;
import com.ondam.question.repository.EveningAnswerRepository;
import com.ondam.question.repository.QuestionTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EveningQuestionService {

    private final EveningQuestionRepository eveningQuestionRepository;
    private final EveningAnswerRepository eveningAnswerRepository;
    private final QuestionTemplateRepository questionTemplateRepository;

}