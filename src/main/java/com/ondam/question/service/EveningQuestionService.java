package com.ondam.question.service;

import com.ondam.global.common.DateUtils;
import com.ondam.global.util.GptClient;
import com.ondam.question.entity.EveningQuestion;
import com.ondam.question.entity.EveningAnswer;
import com.ondam.question.entity.MetricType;
import com.ondam.question.entity.QuestionTemplate;
import com.ondam.question.entity.AnswerType;
import com.ondam.question.repository.EveningAnswerRepository;
import com.ondam.question.repository.EveningQuestionRepository;
import com.ondam.question.repository.QuestionTemplateRepository;
import com.ondam.question.dto.response.EveningQuestionResponse;
import com.ondam.question.dto.response.VoiceTranscribeResponse;
import com.ondam.question.dto.request.EveningAnswerSubmitRequest;
import com.ondam.record.entity.HealthRecord;
import com.ondam.record.entity.SourceType;
import com.ondam.record.repository.HealthRecordRepository;
import com.ondam.user.repository.HealthProfileRepository;
import com.ondam.user.entity.HealthProfile;
import com.ondam.user.entity.WellnessInterest;
import com.ondam.dailylog.service.DailyLogService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.MediaType;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EveningQuestionService {

    private final EveningQuestionRepository eveningQuestionRepository;
    private final EveningAnswerRepository eveningAnswerRepository;
    private final QuestionTemplateRepository questionTemplateRepository;
    private final ObjectMapper objectMapper;

    private final HealthRecordRepository healthRecordRepository;
    private final HealthProfileRepository healthProfileRepository;

    private final DailyLogService dailyLogService;
    private final WebClient openAiWebClient;
    private final GptClient gptClient;

    public EveningQuestionResponse getTodayQuestions(Long userId) {

        LocalDate today = DateUtils.today();

        List<EveningQuestion> questions = eveningQuestionRepository.findByUserIdAndQuestionDate(userId, today);

        if (questions.isEmpty()) {
            questions = generateTodayQuestions(userId, today);
        }

        return buildResponse(questions, userId);
    }

    @Transactional
    public void submitAnswers(Long userId, EveningAnswerSubmitRequest request) {

        for (EveningAnswerSubmitRequest.AnswerItem item : request.answers()) {

            Optional<EveningQuestion> questionOpt = eveningQuestionRepository.findById(item.questionId());

            if (questionOpt.isEmpty()) {
                continue;
            }

            EveningQuestion question = questionOpt.get();

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

            Optional<EveningAnswer> existingOpt = eveningAnswerRepository
                    .findByEveningQuestionIdAndUserId(question.getId(), userId);

            if (existingOpt.isPresent()) {
                // 1. 기존 답변이 있으면 Update
                EveningAnswer existingAnswer = existingOpt.get();
                existingAnswer.update(item.textValue(), item.choiceValue(), item.inputType());

                // 2. 연결된 HealthRecord도 같이 Update
                Optional<HealthRecord> recordOpt = healthRecordRepository.findByEveningAnswerId(existingAnswer.getId());
                if (recordOpt.isPresent()) {
                    recordOpt.get().update(numericValue, item.textValue());
                }
            } else {
                // 1. 없으면 Insert
                EveningAnswer answer = EveningAnswer.builder()
                        .eveningQuestionId(item.questionId())
                        .userId(userId)
                        .textValue(item.textValue())
                        .choiceValue(item.choiceValue())
                        .inputType(item.inputType())
                        .answeredAt(LocalDateTime.now())
                        .build();
                eveningAnswerRepository.save(answer);

                // 2. HealthRecord도 새로 Insert
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
        }

        dailyLogService.refresh(userId, DateUtils.today());
    }

    private List<EveningQuestion> generateTodayQuestions(Long userId, LocalDate today) {

        List<EveningQuestion> questions = new ArrayList<>();

        List<MetricType> types = List.of(MetricType.CONDITION, MetricType.SLEEP,
                MetricType.MEAL, MetricType.ACTIVITY, MetricType.CUSTOM);

        for (MetricType type : types) {

            EveningQuestion question;

            if (type == MetricType.CUSTOM) {
                // GPT로 질문 생성 (실패하면 기존 폴백 템플릿 사용)
                String customContent = generateCustomQuestionByAi(userId);

                question = EveningQuestion.builder()
                        .templateId(null)               // GPT 생성이라 원본 템플릿 없음
                        .userId(userId)
                        .questionDate(today)
                        .metricType(type)
                        .content(customContent)
                        .answerType(AnswerType.TEXT)
                        .choices(null)
                        .build();

            } else {
                List<QuestionTemplate> candidates = questionTemplateRepository.findByMetricTypeAndIsActiveTrue(type);
                QuestionTemplate template = candidates.get(0);

                question = EveningQuestion.builder()
                        .templateId(template.getId())
                        .userId(userId)
                        .questionDate(today)
                        .metricType(type)
                        .content(template.getContent())
                        .answerType(template.getAnswerType())
                        .choices(template.getChoices())
                        .build();
            }

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
                    new TypeReference<List<EveningQuestionResponse.ChoiceItem>>() {
                    });
        } catch (JacksonException e) {
            throw new RuntimeException("choices 파싱 실패", e);
        }
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

    public VoiceTranscribeResponse transcribe(Long questionId, MultipartFile audioFile) {
        String transcript = callWhisperApi(audioFile);
        return new VoiceTranscribeResponse(transcript);
    }

    private String callWhisperApi(MultipartFile audioFile) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", audioFile.getResource());
            builder.part("model", "whisper-1");

            Map<String, Object> response = openAiWebClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return (String) response.get("text");

        } catch (Exception e) {
            throw new RuntimeException("음성 인식 실패", e);
        }
    }

    private String generateCustomQuestionByAi(Long userId) {

        try {
            Optional<HealthProfile> profileOpt = healthProfileRepository.findByUserId(userId);

            String topic;         // 프롬프트에 넣을 실제 주제
            String topicType;     // "지병"인지 "관심사"인지 GPT에게 알려주기 위한 구분

            if (profileOpt.isPresent()
                    && profileOpt.get().getDiseases() != null
                    && !profileOpt.get().getDiseases().isEmpty()) {
                // 지병이 있으면 무조건 지병 우선
                topic = String.join(", ", profileOpt.get().getDiseases());
                topicType = "지병";

            } else if (profileOpt.isPresent()
                    && profileOpt.get().getWellnessInterests() != null
                    && !profileOpt.get().getWellnessInterests().isEmpty()) {
                // 지병이 없으면 관심사 사용
                topic = profileOpt.get().getWellnessInterests().stream()
                        .map(this::translateWellnessInterest)
                        .collect(Collectors.joining(", "));
                topicType = "관심사";

            } else {
                // 둘 다 없으면 일반 질문
                topic = "특별한 지병이나 관심사 없음";
                topicType = "없음";
            }

            String prompt = String.format("""
                당신은 어르신을 위한 건강 체크 앱의 질문 작성자입니다.

                다음 순서로 작업해주세요.

                1단계: 이 사용자의 %s는 '%s'입니다. 이것을 관리하거나 챙길 때 일상에서 특히 신경 써야 할 부분이 무엇인지 생각해보세요.

                2단계: 1단계에서 찾은 정보를 바탕으로, 다음 두 가지 중 하나의 방향으로 질문을 만드세요.
                - 방향 A: 이와 관련된 증상이나 변화가 오늘 있었는지 확인하는 질문
                - 방향 B: 관리나 개선에 도움되는 특정 행동을 오늘 실천했는지 확인하는 질문
                두 방향 중 더 적합한 쪽을 하나만 골라서 질문을 만드세요.

                3단계: 그 질문을 따뜻하고 부담 없는 말투로, 한 문장으로만 다듬어서 최종 작성하세요.

                최종 결과 형식:
                - "오늘 ~하셨나요?" 형태의 사실 확인 질문으로 작성하세요. (예: "오늘 짠 음식을 얼마나 드셨는지 편하게 말씀해주세요.")
                - "괜찮으세요?", "걱정되지 않으세요?" 같은 감정을 묻는 질문은 피하세요.
                - 여러 개면, 그중 가장 중요한 것 하나만 골라서 질문하세요.
                - 최종 질문 문장 하나만 출력하고, 1~3단계 과정이나 다른 설명은 절대 출력하지 마세요.
                """, topicType, topic);

            return gptClient.ask(prompt).trim();

        } catch (Exception e) {
            QuestionTemplate fallback = pickCustomTemplate(userId, MetricType.CUSTOM);
            return fallback.getContent();
        }
    }

    private String translateWellnessInterest(WellnessInterest interest) {
        return switch (interest) {
            case SLEEP -> "수면";
            case ACTIVITY -> "활동량";
            case MEAL -> "식사";
            case MEDICINE -> "복약 관리";
            case MOOD -> "기분/정서";
        };
    }
}