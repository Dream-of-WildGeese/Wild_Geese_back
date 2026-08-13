package com.ondam.question.service;

import com.ondam.global.common.DateUtils;
import com.ondam.question.entity.MorningQuestion;
import com.ondam.question.entity.MorningAnswer;
import com.ondam.question.entity.InputType;
import com.ondam.question.repository.MorningQuestionRepository;
import com.ondam.question.repository.MorningAnswerRepository;
import com.ondam.question.dto.request.MorningAnswerRequest;
import com.ondam.question.dto.response.MorningQuestionHistoryItem;
import com.ondam.user.entity.User;
import com.ondam.user.repository.UserRepository;
import com.ondam.family.entity.Family;
import lombok.RequiredArgsConstructor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeFamilyInformation;
import org.springframework.stereotype.Service;

import com.ondam.question.dto.response.MorningQuestionResponse;
import com.ondam.user.entity.HealthProfile;
import com.ondam.user.repository.HealthProfileRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MorningQuestionService {

    private final MorningQuestionRepository morningQuestionRepository;
    private final MorningAnswerRepository morningAnswerRepository;
    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;

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
            // TODO: a.getUserId()로 HealthProfileRepository 조회

            Optional<HealthProfile> profileOpt = healthProfileRepository.findByUserId(a.getUserId());

            // TODO: profile이 있으면 name/role 꺼내고, 없으면 기본값("알 수 없음", null)

            String name = "Unknown";
            String role = null;

            if(profileOpt.isPresent()){
                name = profileOpt.get().getName();
                role = profileOpt.get().getRole().toString();
            }

            // TODO: FamilyAnswerItem.builder()로 조각 하나 만들어서 familyAnswerItems에 add

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

        String content = "오늘 점심에 가장 먹고 싶은 음식은?";  // TODO: 나중에 AI로 생성하도록 교체

        MorningQuestion question = MorningQuestion.builder()
                .familyId(familyId)
                .questionDate(today)
                .content(content)
                .build();

        return morningQuestionRepository.save(question);
    }

    public void submitAnswer(Long userId, Long questionId, MorningAnswerRequest request) {

        MorningQuestion question = morningQuestionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("질문이 존재하지 않습니다."));

        MorningAnswer answer = MorningAnswer.builder()
                .morningQuestionId(question.getId())
                .userId(userId)
                .textValue(request.textValue())
                .inputType(request.inputType())
                .answeredAt(LocalDateTime.now())
                .build();

        morningAnswerRepository.save(answer);
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