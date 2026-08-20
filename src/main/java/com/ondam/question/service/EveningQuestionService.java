package com.ondam.question.service;

import com.ondam.notification.entity.NotificationType;
import com.ondam.notification.service.NotificationService;
import com.ondam.notification.service.WebPushService;
import com.ondam.global.common.DateUtils;
import com.ondam.global.util.GptClient;
import com.ondam.global.util.SttClient;
import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
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
import com.ondam.report.service.WeeklyReportService;
import com.ondam.user.repository.HealthProfileRepository;
import com.ondam.user.repository.NotificationSettingRepository;
import com.ondam.user.repository.UserRepository;
import com.ondam.user.entity.HealthProfile;
import com.ondam.user.entity.WellnessInterest;
import com.ondam.user.entity.NotificationSetting;
import com.ondam.user.entity.User;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
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

    private final DailyLogService dailyLogService;
    private final GptClient gptClient;
    private final SttClient sttClient;

    private final WeeklyReportService weeklyReportService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationSettingRepository notificationSettingRepository;
    private final WebPushService webPushService;

    public EveningQuestionResponse getTodayQuestions(Long userId) {

        LocalDate today = DateUtils.today();

        List<EveningQuestion> questions = eveningQuestionRepository.findByUserIdAndQuestionDate(userId, today);

        if (questions.isEmpty()) {
            questions = generateTodayQuestions(userId, today);
        }

        return buildResponse(questions, userId);
    }

    public void submitAnswers(Long userId, EveningAnswerSubmitRequest request) {

        saveAnswers(userId, request);   // 트랜잭션 있는 저장 로직만

        LocalDate today = DateUtils.today();
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            notifyWeeklyReportToFamily(userId, today);   // 트랜잭션 없이, DB 저장 끝난 뒤에 실행
        }
    }

    @Transactional
    public void saveAnswers(Long userId, EveningAnswerSubmitRequest request) {

        for (EveningAnswerSubmitRequest.AnswerItem item : request.answers()) {

            Optional<EveningQuestion> questionOpt = eveningQuestionRepository.findById(item.questionId());

            if (questionOpt.isEmpty()) {
                continue;
            }

            EveningQuestion question = questionOpt.get();

            // choices에서 numericValue 찾기
            BigDecimal numericValue = null;
            if (item.choiceValue() != null) {
                try {
                    numericValue = new BigDecimal(item.choiceValue());
                } catch (NumberFormatException e) {
                    // choiceValue가 숫자가 아니면 무시
                }
            }

            Optional<EveningAnswer> existingOpt = eveningAnswerRepository
                    .findFirstByEveningQuestionIdAndUserId(question.getId(), userId);

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

    public void notifyWeeklyReportToFamily(Long userId, LocalDate today) {

        weeklyReportService.getWeeklyReport(userId, today);

        User me = userRepository.findById(userId).orElseThrow();
        Long familyId = me.getFamily().getId();
        List<User> familyMembers = userRepository.findAllByFamilyId(familyId);

        for (User member : familyMembers) {
            if (!member.getId().equals(userId)) {
                NotificationSetting setting = notificationSettingRepository.findByUserId(member.getId()).orElse(null);

                if (setting != null && setting.isReportEnabled()) {
                    String title = "가족 주간 리포트 도착";
                    String content = me.getName() + "님의 이번 주 건강 리포트가 완성되었어요. 확인해보세요!";

                    notificationService.createNotification(
                            member.getId(), NotificationType.WEEKLY_REPORT, title, content, LocalDateTime.now());
                    webPushService.sendPush(member.getId(), title, content);
                }
            }
        }
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

                if (candidates.isEmpty()) {
                    throw new BusinessException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND);  // 적절한 에러코드 필요
                }
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

            Optional<EveningAnswer> answer = eveningAnswerRepository.findFirstByEveningQuestionIdAndUserId(q.getId(), userId);

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
                .totalCount(questions.size())
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

        List<QuestionTemplate> fallback = questionTemplateRepository
                .findByMetricTypeAndTargetDiseaseIsNullAndIsActiveTrue(type);

        if (fallback.isEmpty()) {
            throw new BusinessException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND);
        }

        return fallback.get(0);
    }

    public VoiceTranscribeResponse transcribe(Long questionId, MultipartFile audioFile) {
        String transcript = sttClient.transcribe(audioFile);
        return new VoiceTranscribeResponse(transcript);
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

            LocalDate today = DateUtils.today();
            Optional<EveningQuestion> lastQuestionOpt = eveningQuestionRepository
                    .findFirstByUserIdAndMetricTypeAndQuestionDateBeforeOrderByQuestionDateDesc(userId, MetricType.CUSTOM, today);

            String previousContext = "";
            if (lastQuestionOpt.isPresent()) {
                Optional<EveningAnswer> lastAnswerOpt = eveningAnswerRepository
                        .findFirstByEveningQuestionIdAndUserId(lastQuestionOpt.get().getId(), userId);

                // 이전 답변 중 텍스트(STT 등) 기록이 존재한다면 프롬프트에 추가
                if (lastAnswerOpt.isPresent() && lastAnswerOpt.get().getTextValue() != null) {
                    previousContext = String.format("\n[이전 답변 기록]\n지난번(%s)에 사용자는 '%s'라는 질문에 다음과 같이 답했습니다: '%s'\n",
                            lastQuestionOpt.get().getQuestionDate(),
                            lastQuestionOpt.get().getContent(),
                            lastAnswerOpt.get().getTextValue());
                }
            }

            String prompt = String.format("""
                당신은 어르신을 위한 건강 체크 앱의 다정한 질문 작성자입니다.

                다음 순서로 작업해주세요.
                
                1단계: 이 사용자의 %s는 '%s'입니다. %s
                이것을 관리하거나 챙길 때 일상에서 특히 신경 써야 할 부분이 무엇인지 생각해보세요.

                2단계: 1단계 정보를 바탕으로 아래 기준에 따라 질문의 방향을 결정하세요.
                - [이전 답변 기록]이 있는 경우: 지난 답변 내용을 자연스럽게 언급하며(예: "지난번에 무릎이 아프다고 하셨는데..."), 그 증상이나 상태가 오늘은 어떤지 확인하는 '꼬리물기 질문'을 만드세요.
                - [이전 답변 기록]이 없는 경우: 다음 두 가지 중 더 적합한 쪽을 하나만 고르세요.
                  방향 A: 질환/관심사와 관련된 증상이나 불편한 변화가 오늘 있었는지 확인하는 질문
                  방향 B: 관리나 개선에 도움 되는 특정 행동을 오늘 실천했는지 확인하는 질문

                3단계: 결정된 내용을 따뜻하고 부담 없는 말투로, 단 한 문장으로 다듬어서 최종 작성하세요.

                [최종 결과 형식 규칙]
                    - "오늘 ~하셨나요?", "오늘 ~는 어떠셨나요?" 형태의 짧고 간결한 사실 확인 질문으로 작성하세요. (예: "오늘 짠 음식을 얼마나 드셨나요?", "오늘 무릎 통증은 어떠셨나요?")
                    - 존댓말은 유지하되, "~해주실 수 있을까요?", "편하게 말씀해주세요" 같은 길고 부드럽게 돌려 말하는 극존칭 표현은 쓰지 마세요. 간결하고 자연스러운 존댓말로 작성하세요.
                    - 한 문장은 25자를 넘기지 않도록 최대한 짧게 작성하세요.
                    - "괜찮으세요?", "걱정되지 않으세요?" 같이 막연한 불안감을 유발하거나 감정을 묻는 표현은 절대 피하세요.
                    - 질문을 여러 개 던지지 말고, 가장 중요한 것 딱 하나만 물어보세요.
                    - 최종 질문 문장 하나만 출력하고, 1~3단계 사고 과정이나 부가 설명, 인사말, 따옴표는 절대 출력하지 마세요.
                """, topicType, topic, previousContext);

            return gptClient.ask(prompt).trim();

        } catch (Exception e) {
            System.err.println("[CUSTOM_DEBUG] GPT 생성 실패: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();

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