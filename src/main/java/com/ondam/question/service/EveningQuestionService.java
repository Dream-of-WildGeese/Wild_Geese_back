package com.ondam.question.service;

import com.ondam.global.common.DateUtils;
import com.ondam.question.entity.EveningQuestion;
import com.ondam.question.entity.EveningAnswer;
import com.ondam.question.entity.MetricType;
import com.ondam.question.entity.QuestionTemplate;
import com.ondam.question.repository.EveningAnswerRepository;
import com.ondam.question.repository.EveningQuestionRepository;
import com.ondam.question.repository.QuestionTemplateRepository;
import com.ondam.question.dto.response.EveningQuestionResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EveningQuestionService {

    private final EveningQuestionRepository eveningQuestionRepository;
    private final EveningAnswerRepository eveningAnswerRepository;
    private final QuestionTemplateRepository questionTemplateRepository;
    private final ObjectMapper objectMapper;

    public EveningQuestionResponse getTodayQuestions(Long userId){

        LocalDate today = DateUtils.today();

        List<EveningQuestion> questions = eveningQuestionRepository.findByUserIdAndQuestionDate(userId, today);

        if (questions.isEmpty()){
            questions = generateTodayQuestions(userId, today);
        }

        return buildResponse(questions, userId);
    }

    private List<EveningQuestion> generateTodayQuestions(Long userId, LocalDate today){
        List<EveningQuestion> questions = new ArrayList<>();

        List<MetricType> types = List.of(MetricType.CONDITION, MetricType.SLEEP,
                MetricType.MEAL, MetricType.ACTIVITY, MetricType.BODY);

        for(MetricType type : types){
            List<QuestionTemplate> candidates = questionTemplateRepository.findByMetricTypeAndIsActiveTrue(type);

            QuestionTemplate template = candidates.get(0);

            EveningQuestion question = EveningQuestion.builder()
                    .templateId(template.getId())
                    .userId(userId)
                    .questionDate(today)
                    .metricType(type)
                    .content(template.getContent())
                    .answerType(template.getAnswerType())
                    .choices(template.getChoices())
                    .build();

            EveningQuestion saved = eveningQuestionRepository.save(question);
            questions.add(saved);
        }

        return questions;
    }

    private EveningQuestionResponse buildResponse(List<EveningQuestion> questions, Long userId) {

        int completedCount = 0;
        List<EveningQuestionResponse.QuestionItem> items = new ArrayList<>();

        for (EveningQuestion q : questions) {

            Optional<EveningAnswer> answer = eveningAnswerRepository.findByEveningQuestionIdAndUserId(q.getId(), userId);

            Object myAnswer = null;
            if (answer.isPresent()) {
                completedCount++;
                myAnswer = answer.get().getChoiceValue() != null
                        ? answer.get().getChoiceValue()
                        : answer.get().getTextValue();
            }

            EveningQuestionResponse.QuestionItem item = EveningQuestionResponse.QuestionItem.builder()
                    .questionId(q.getId())
                    .metricType(q.getMetricType())
                    .content(q.getContent())
                    .answerType(q.getAnswerType())
                    .choices(parseChoices(q.getChoices()))
                    .myAnswer(myAnswer)
                    .build();

            items.add(item);
        }

        // 4. 최종 EveningQuestionResponse를 builder로 조립해서 return
        return EveningQuestionResponse.builder()
                .questionDate(questions.get(0).getQuestionDate().toString())
                .completedCount(completedCount)
                .totalCount(5)
                .questions(items)
                .build();
    }

    private List<EveningQuestionResponse.ChoiceItem> parseChoices(String choicesJson) {
        if (choicesJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(choicesJson,
                    new TypeReference<List<EveningQuestionResponse.ChoiceItem>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("choices 파싱 실패", e);
        }
    }
}