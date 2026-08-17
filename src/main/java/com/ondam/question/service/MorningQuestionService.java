package com.ondam.question.service;

import com.ondam.notification.entity.NotificationType;
import com.ondam.notification.service.NotificationService;
import com.ondam.notification.service.WebPushService;
import com.ondam.user.entity.NotificationSetting;
import com.ondam.user.repository.NotificationSettingRepository;
import com.ondam.global.common.DateUtils;
import com.ondam.global.util.GptClient;
import com.ondam.question.entity.MorningQuestion;
import com.ondam.question.entity.MorningAnswer;
import com.ondam.question.repository.MorningQuestionRepository;
import com.ondam.question.repository.MorningAnswerRepository;
import com.ondam.question.dto.request.MorningAnswerRequest;
import com.ondam.question.dto.response.MorningQuestionHistoryItem;
import com.ondam.question.dto.response.MorningQuestionResponse;
import com.ondam.user.entity.User;
import com.ondam.user.repository.UserRepository;
import com.ondam.family.entity.Family;
import com.ondam.dailylog.service.DailyLogService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class MorningQuestionService {

    private final MorningQuestionRepository morningQuestionRepository;
    private final MorningAnswerRepository morningAnswerRepository;
    private final UserRepository userRepository;
    private final DailyLogService dailyLogService;

    private final NotificationService notificationService;
    private final NotificationSettingRepository notificationSettingRepository;
    private final WebPushService webPushService;

    private final GptClient gptClient;

    public MorningQuestionResponse getTodayQuestion(Long userId) {
        // 1. userId로 User 조회 → Family 얻기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다."));

        Family family = user.getFamily();
        Long familyId = family.getId();

        // 2. familyId + 오늘 날짜로 질문 조회
        LocalDate today = DateUtils.today();

        Optional<MorningQuestion> questionOpt = morningQuestionRepository
                .findByFamilyIdAndQuestionDate(familyId, today);

        MorningQuestion question;

        if (questionOpt.isPresent()) {
            question = questionOpt.get();
        } else {
            question = generateTodayQuestion(familyId, today);
        }

        // 3. 그 질문에 대한 가족 전원의 답변 조회
        List<MorningAnswer> familyAnswerList = morningAnswerRepository.findByMorningQuestionId(question.getId());

        // 4. 내 답변 따로 조회
        Optional<MorningAnswer> myAnswerOpt = morningAnswerRepository
                .findByMorningQuestionIdAndUserId(question.getId(), userId);
        // 5. 응답 조립

        List<MorningQuestionResponse.FamilyAnswerItem> familyAnswerItems = new ArrayList<>();

        for (MorningAnswer a : familyAnswerList) {

            Optional<User> answerUserOpt = userRepository.findById(a.getUserId());

            String name = "Unknown";
            String role = null;

            if(answerUserOpt.isPresent()){
                name = answerUserOpt.get().getName();
                role = answerUserOpt.get().getRole().toString();
            }

            MorningQuestionResponse.FamilyAnswerItem item = MorningQuestionResponse.FamilyAnswerItem.builder()
                    .userId(a.getUserId())
                    .name(name)
                    .role(role)
                    .textValue(a.getTextValue())
                    .answeredAt(a.getAnsweredAt())
                    .build();

            familyAnswerItems.add(item);
        }

        // 5-2. 내 답변 텍스트만 뽑기 (없으면 null)
        String myAnswer = myAnswerOpt.isPresent() ? myAnswerOpt.get().getTextValue() : null;

        // 5-3. 최종 DTO 조립해서 반환
        return MorningQuestionResponse.builder()
                .questionId(question.getId())
                .questionDate(question.getQuestionDate().toString())
                .content(question.getContent())
                .myAnswer(myAnswer)
                .familyAnswers(familyAnswerItems)
                .build();
    }

    private MorningQuestion generateTodayQuestion(Long familyId, LocalDate today) {

        String content = generateMorningQuestionByAi();

        MorningQuestion question = MorningQuestion.builder()
                .familyId(familyId)
                .questionDate(today)
                .content(content)
                .build();

        return morningQuestionRepository.save(question);
    }

    // ai 질문 생성 메서드
    private String generateMorningQuestionByAi() {
        try {
            String prompt = """
                당신은 가족 간의 따뜻한 소통을 돕는 헬스케어 앱의 질문 작성자입니다.
                가족들이 아침에 서로 안부를 묻거나 가볍게 대화를 시작할 수 있는 기분 좋은 질문 1개를 작성해주세요.

                [작성 지침]
                1. 무겁거나 심각한 주제는 피하고, 일상적이고 긍정적인 주제(예: 식사, 어릴 적 추억, 오늘의 기분, 날씨, 소소한 계획 등)를 선택하세요.
                2. 어르신(부모님)과 자녀 세대가 모두 쉽게 공감하고 대답할 수 있는 질문이어야 합니다.
                3. 부드럽고 다정한 존댓말로 작성하세요.
                4. 부가적인 설명, 따옴표, 인사말 없이 오직 질문 딱 한 문장만 출력하세요.
                """;
            return gptClient.ask(prompt).trim();

        } catch (Exception e) {
            // GPT API 실패 시 기본적으로 돌려줄 안전한 폴백(Fallback) 질문 리스트
            List<String> fallbacks = List.of(
                    "오늘은 어떤 메뉴를 드시고 싶으신가요?",
                    "오늘 하루, 가장 기대되는 일은 무엇인가요?",
                    "최근에 가장 흥미있는 관심사가 무엇인가요?",
                    "가장 좋아하는 계절과 그 이유는 무엇인가요?",
                    "최근에 가족과 함께 먹고 싶은 음식이 있다면 무엇인가요?"
            );
            return fallbacks.get(new Random().nextInt(fallbacks.size()));
        }
    }

    @Transactional
    public void submitAnswer(Long userId, Long questionId, MorningAnswerRequest request) {

        MorningQuestion question = morningQuestionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("질문이 존재하지 않습니다."));

        Optional<MorningAnswer> existingOpt = morningAnswerRepository
                .findByMorningQuestionIdAndUserId(question.getId(), userId);

        if (existingOpt.isPresent()) {
            // 이미 답변이 있으면 덮어쓰기 (Update)
            existingOpt.get().update(request.textValue(), request.inputType());
        } else {
            // 없으면 새로 저장 (Insert)
            MorningAnswer answer = MorningAnswer.builder()
                    .morningQuestionId(question.getId())
                    .userId(userId)
                    .textValue(request.textValue())
                    .inputType(request.inputType())
                    .answeredAt(LocalDateTime.now())
                    .build();
            morningAnswerRepository.save(answer);
        }

        User me = userRepository.findById(userId).orElseThrow();
        Long familyId = me.getFamily().getId();

        List<User> familyMembers = userRepository.findAllByFamilyId(familyId);

        for (User member : familyMembers) {
            // 본인에게는 알림을 보내지 않음
            if (!member.getId().equals(userId)) {
                NotificationSetting setting = notificationSettingRepository.findByUserId(member.getId()).orElse(null);

                // 가족 반응 알림을 켜둔 구성원에게만
                if (setting != null && setting.isFamilyReactionEnabled()) {
                    String title = "가족의 답변이 등록되었어요!";
                    String content = me.getName() + "님이 오늘의 아침 질문에 답변을 남겼습니다.";

                    notificationService.createNotification(member.getId(), NotificationType.MORNING_QUESTION, title, content, java.time.LocalDateTime.now());
                    webPushService.sendPush(member.getId(), title, content);
                }
            }
        }

        dailyLogService.refresh(userId, DateUtils.today());
    }

    public List<MorningQuestionHistoryItem> getMorningHistory(Long userId, LocalDate from, LocalDate to) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다."));

        Family family = user.getFamily();
        Long familyId = family.getId();

        // 2. morningQuestionRepository로 범위 조회

        List<MorningQuestion> questions = morningQuestionRepository
                .findByFamilyIdAndQuestionDateBetween(familyId, from, to);

        // 3. 각 MorningQuestion을 응답 형태로 변환해서 리스트로 반환

        List<MorningQuestionHistoryItem> result = new ArrayList<>();

        for (MorningQuestion q : questions) {

            MorningQuestionHistoryItem item = MorningQuestionHistoryItem.builder()
                    .questionId(q.getId())
                    .questionDate(q.getQuestionDate().toString())
                    .content(q.getContent())
                    .build();

            result.add(item);
        }

        return result;
    }
}