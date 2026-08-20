package com.ondam.question.service;

import com.ondam.notification.entity.NotificationType;
import com.ondam.notification.service.NotificationService;
import com.ondam.notification.service.WebPushService;
import com.ondam.user.entity.NotificationSetting;
import com.ondam.user.repository.NotificationSettingRepository;
import com.ondam.global.common.DateUtils;
import com.ondam.global.util.GptClient;
import com.ondam.global.util.SttClient;
import com.ondam.global.util.S3Uploader;
import com.ondam.question.entity.MorningQuestion;
import com.ondam.question.entity.MorningAnswer;
import com.ondam.question.entity.MorningReaction;
import com.ondam.question.entity.InputType;
import com.ondam.question.repository.MorningReactionRepository;
import com.ondam.question.repository.MorningQuestionRepository;
import com.ondam.question.repository.MorningAnswerRepository;
import com.ondam.question.dto.request.MorningAnswerRequest;
import com.ondam.question.dto.response.MorningQuestionHistoryItem;
import com.ondam.question.dto.response.MorningQuestionResponse;
import com.ondam.question.dto.response.MorningAnswerResponse;
import com.ondam.user.entity.User;
import com.ondam.user.repository.UserRepository;
import com.ondam.family.entity.Family;
import com.ondam.dailylog.service.DailyLogService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Map;

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
    private final MorningReactionRepository morningReactionRepository;

    private final GptClient gptClient;
    private final S3Uploader s3Uploader;
    private final SttClient sttClient;

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
                .findFirstByMorningQuestionIdAndUserId(question.getId(), userId);
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
                    .answerId(a.getId())
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
            List<String> categories = List.of(
                    "과거 회상 (어릴 적 추억, 옛날 이야기)",
                    "미래 계획 (다가올 일정, 하고 싶은 것)",
                    "오늘의 일상 (식사, 날씨, 오늘 할 일)",
                    "취향과 기호 (좋아하는 것, 싫어하는 것)",
                    "가족과의 추억 (예전에 함께했던 일)"
            );
            String category = categories.get(new Random().nextInt(categories.size()));

            String prompt = String.format("""
                당신은 가족 간의 따뜻한 소통을 돕는 헬스케어 앱의 질문 작성자입니다.
                가족들이 아침에 서로 안부를 묻거나 가볍게 대화를 시작할 수 있는 질문 1개를 작성해주세요.

                오늘의 질문 주제는 다음 카테고리로 한정합니다: %s

                [작성 지침]
                1. 반드시 위 카테고리 안에서만 질문을 작성하세요. 다른 주제로 벗어나지 마세요.
                2. 무겁거나 심각한 주제는 피하고, 긍정적이고 편안한 어투로 작성하세요.
                3. 어르신(부모님)과 자녀 세대가 모두 쉽게 공감하고 대답할 수 있는 질문이어야 합니다.
                4. 부드럽고 다정한 존댓말로 작성하세요.
                5. 부가적인 설명, 따옴표, 인사말 없이 오직 질문 딱 한 문장만 출력하세요.
                """, category);

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
                .findFirstByMorningQuestionIdAndUserId(question.getId(), userId);

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

    @Transactional
    public void toggleReaction(Long userId, Long answerId, String emoji) {

        Optional<MorningReaction> existing =
                morningReactionRepository
                        .findByMorningAnswerIdAndUserIdAndEmoji(
                                answerId,
                                userId,
                                emoji
                        );

        if (existing.isPresent()) {

            // 이미 누른 이모지면 삭제
            morningReactionRepository.delete(existing.get());

        } else {

            // 1. 반응 저장
            MorningReaction reaction =
                    MorningReaction.builder()
                            .morningAnswerId(answerId)
                            .userId(userId)
                            .emoji(emoji)
                            .build();

            morningReactionRepository.save(reaction);

            // 2. 반응 대상 답변 조회
            MorningAnswer answer =
                    morningAnswerRepository.findById(answerId)
                            .orElseThrow(() ->
                                    new RuntimeException("아침 답변을 찾을 수 없습니다.")
                            );

            // 3. 알림 받을 사람 = 답변 작성자
            Long receiverUserId = answer.getUserId();

            // 자기 답변에 자기 반응이면 알림 안 보냄
            if (receiverUserId.equals(userId)) {
                return;
            }

            // 4. 상대방의 가족 반응 알림 설정 확인
            NotificationSetting setting =
                    notificationSettingRepository
                            .findByUserId(receiverUserId)
                            .orElse(null);

            if (setting == null || !setting.isFamilyReactionEnabled()) {
                return;
            }

            // 5. 반응한 사람 조회
            User sender =
                    userRepository.findById(userId)
                            .orElseThrow(() ->
                                    new RuntimeException("사용자를 찾을 수 없습니다.")
                            );

            String title = "가족이 반응을 남겼어요!";
            String content =
                    sender.getName()
                            + "님이 회원님의 아침 답변에 "
                            + translateEmoji(emoji)
                            + " 반응을 남겼어요.";

            // 6. 앱 내부 알림 저장
            notificationService.createNotification(
                    receiverUserId,
                    NotificationType.FAMILY_REACTION,
                    title,
                    content,
                    LocalDateTime.now()
            );

            // 7. 실제 Web Push
            webPushService.sendPush(
                    receiverUserId,
                    title,
                    content
            );
        }
    }

    @Transactional
    public MorningAnswerResponse createAnswerWithStt(MultipartFile audioFile, Long questionId, Long userId) {

        String audioUrl = s3Uploader.upload(audioFile, "morning-answers");
        String transcript = sttClient.transcribe(audioFile);

        Optional<MorningAnswer> existingOpt = morningAnswerRepository
                .findFirstByMorningQuestionIdAndUserId(questionId, userId);

        MorningAnswer savedAnswer;

        if (existingOpt.isPresent()) {
            MorningAnswer existingAnswer = existingOpt.get();
            existingAnswer.update(transcript, InputType.VOICE, audioUrl);
            savedAnswer = existingAnswer;
        } else {
            MorningAnswer answer = MorningAnswer.builder()
                    .morningQuestionId(questionId)
                    .userId(userId)
                    .textValue(transcript)
                    .inputType(InputType.VOICE)
                    .answeredAt(LocalDateTime.now())
                    .audioUrl(audioUrl)
                    .build();
            savedAnswer = morningAnswerRepository.save(answer);
        }

        return new MorningAnswerResponse(
                savedAnswer.getId(),
                savedAnswer.getAudioUrl(),
                savedAnswer.getTextValue()
        );
    }

    private String translateEmoji(String emoji) {
        if (emoji == null) return "";
        return switch (emoji.toUpperCase()) {
            case "CHEER" -> "응원";
            case "CONGRATS" -> "축하";
            case "BEST" -> "최고";
            case "FUNNY" -> "재미있어요";
            case "LIKE" -> "좋아요";
            default -> emoji;   // 방어용, 모르는 값이 오면 원문 그대로
        };
    }
}