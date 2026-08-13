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
import com.ondam.question.dto.request.EveningAnswerSubmitRequest;
import com.ondam.record.entity.HealthRecord;
import com.ondam.record.entity.SourceType;
import com.ondam.record.repository.HealthRecordRepository;
import com.ondam.user.repository.HealthProfileRepository;
import com.ondam.user.entity.HealthProfile;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EveningQuestionService {

    private final EveningQuestionRepository eveningQuestionRepository;
    private final EveningAnswerRepository eveningAnswerRepository;
    private final QuestionTemplateRepository questionTemplateRepository;
    private final ObjectMapper objectMapper;

    private final HealthRecordRepository healthRecordRepository;
    private final HealthProfileRepository healthProfileRepository;

    public EveningQuestionResponse getTodayQuestions(Long userId){

        LocalDate today = DateUtils.today();

        List<EveningQuestion> questions = eveningQuestionRepository.findByUserIdAndQuestionDate(userId, today);

        if (questions.isEmpty()){
            questions = generateTodayQuestions(userId, today);
        }

        return buildResponse(questions, userId);
    }

    public void submitAnswers(Long userId, EveningAnswerSubmitRequest request) {

        for (EveningAnswerSubmitRequest.AnswerItem item : request.answers()) {

            Optional<EveningQuestion> questionOpt = eveningQuestionRepository.findById(item.questionId());

            if (questionOpt.isEmpty()) {
                continue;
            }

            EveningQuestion question = questionOpt.get();

            EveningAnswer answer = EveningAnswer.builder()
                    .eveningQuestionId(item.questionId())
                    .userId(userId)
                    .textValue(item.textValue())
                    .choiceValue(item.choiceValue())
                    .inputType(item.inputType())
                    .answeredAt(LocalDateTime.now())
                    .build();

            eveningAnswerRepository.save(answer);

            // choices에서 numericValue 찾기
            BigDecimal numericValue = null;

            if (item.choiceValue() != null) {
                List<EveningQuestionResponse.ChoiceItem> choices = parseChoices(question.getChoices());

                if (choices != null) {
                    for (EveningQuestionResponse.ChoiceItem choice : choices) {
                        if (choice.label().equals(item.choiceValue())) {
                            numericValue = BigDecimal.valueOf(choice.value());
                            break;
                        }
                    }
                }
            }

            HealthRecord healthRecord = HealthRecord.builder()
                    .userId(userId)
                    .recordDate(DateUtils.today())
                    .metricType(question.getMetricType())
                    .numericValue(numericValue)
                    .textValue(item.textValue())
                    .source(SourceType.ANSWER)
                    .eveningAnswerId(answer.getId())
                    .build();

            healthRecordRepository.save(healthRecord);
        }

        // TODO: dailyLogService.refresh(userId, DateUtils.today());
    }

    private List<EveningQuestion> generateTodayQuestions(Long userId, LocalDate today) {

        List<EveningQuestion> questions = new ArrayList<>();

        List<MetricType> types = List.of(MetricType.CONDITION, MetricType.SLEEP,
                MetricType.MEAL, MetricType.ACTIVITY, MetricType.CUSTOM);

        for (MetricType type : types) {

            QuestionTemplate template;

            if(type == MetricType.CUSTOM){
                template = pickCustomTemplate(userId, type);   // 새로 만들 메서드
            } else{
                List<QuestionTemplate> candidates = questionTemplateRepository.findByMetricTypeAndIsActiveTrue(type);
                template = candidates.get(0);
            }

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
        } catch (JacksonException e) {
            throw new RuntimeException("choices 파싱 실패", e);
        }
    }

    public String transcribe(MultipartFile audioFile) {
        // TODO: 실제 STT API(Whisper, 클로바 스피치 등) 연동 필요
        // 지금은 스텁으로 고정 문자열 반환
        return "음성 인식 결과 예시";
    }

    private QuestionTemplate pickCustomTemplate(Long userId, MetricType type) {

        Optional<HealthProfile> profileOpt = healthProfileRepository.findByUserId(userId);

        if (profileOpt.isPresent()) {
            HealthProfile profile = profileOpt.get();
            List<String> diseases = profile.getDiseases();

            if (diseases != null && !diseases.isEmpty()) {
                for (String disease : diseases) {
                    List<QuestionTemplate> matched = questionTemplateRepository
                            .findByMetricTypeAndTargetDiseaseAndIsActiveTrue(type, disease);

                    if (!matched.isEmpty()) {
                        return matched.get(0);   // 찾았으면 바로 반환하고 메서드 종료
                    }
                }
            }
        }

        List<QuestionTemplate> fallback = questionTemplateRepository.findByMetricTypeAndIsActiveTrue(type);
        return fallback.get(0);
    }
}