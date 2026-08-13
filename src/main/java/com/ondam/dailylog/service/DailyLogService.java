package com.ondam.dailylog.service;

import com.ondam.dailylog.entity.DailyLog;
import com.ondam.dailylog.repository.DailyLogRepository;
import com.ondam.dailylog.dto.response.DailyLogResponse;
import com.ondam.question.entity.MorningQuestion;
import com.ondam.question.entity.MorningAnswer;
import com.ondam.question.entity.EveningQuestion;
import com.ondam.question.entity.EveningAnswer;
import com.ondam.question.repository.MorningAnswerRepository;
import com.ondam.question.repository.MorningQuestionRepository;
import com.ondam.question.repository.EveningAnswerRepository;
import com.ondam.question.repository.EveningQuestionRepository;
import com.ondam.user.entity.User;
import com.ondam.user.repository.UserRepository;
import com.ondam.family.entity.Family;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import tools.jackson.core.type.TypeReference;
import tools.jackson.core.JacksonException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class DailyLogService {

    private final DailyLogRepository dailyLogRepository;
    private final MorningAnswerRepository morningAnswerRepository;
    private final MorningQuestionRepository morningQuestionRepository;
    private final EveningAnswerRepository eveningAnswerRepository;
    private final EveningQuestionRepository eveningQuestionRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;   // ← 이 줄 추가

    public void refresh(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다."));

        Family family = user.getFamily();
        Long familyId = family.getId();

        Optional<MorningQuestion> questionOpt = morningQuestionRepository
                .findByFamilyIdAndQuestionDate(familyId, date);

        MorningAnswer morningAnswer = null;

        if(questionOpt.isPresent()){
            Optional<MorningAnswer> answerOpt = morningAnswerRepository
                    .findByMorningQuestionIdAndUserId(questionOpt.get().getId(), userId);
            if(answerOpt.isPresent()){
                morningAnswer = answerOpt.get();
            }
        }

        List<EveningQuestion> eveningQuestions = eveningQuestionRepository
                .findByUserIdAndQuestionDate(userId, date);

        List<EveningAnswer> eveningAnswers = new ArrayList<>();

        for (EveningQuestion eq : eveningQuestions) {
            Optional<EveningAnswer> answerOpt = eveningAnswerRepository
                    .findByEveningQuestionIdAndUserId(eq.getId(), userId);
            if (answerOpt.isPresent()) {
                eveningAnswers.add(answerOpt.get());
            }
        }

        String morningAnswerJson;
        String eveningAnswersJson;

        try {
            morningAnswerJson = morningAnswer != null
                    ? objectMapper.writeValueAsString(morningAnswer)
                    : null;

            eveningAnswersJson = objectMapper.writeValueAsString(eveningAnswers);

        } catch (JacksonException e) {
            throw new RuntimeException("DailyLog JSON 변환 실패", e);
        }

        boolean morningAnswered = (morningAnswer != null);
        int eveningCompletedCount = eveningAnswers.size();
        int eveningTotalCount = eveningQuestions.size();

        Optional<DailyLog> existingLogOpt = dailyLogRepository.findByUserIdAndLogDate(userId, date);

        if (existingLogOpt.isPresent()) {
            // 이미 있으면 → 기존 걸 지우고 새로 만들어서 저장 (가장 간단한 방법)
            dailyLogRepository.delete(existingLogOpt.get());
        }

        DailyLog dailyLog = DailyLog.builder()
                .userId(userId)
                .logDate(date)
                .morningAnswer(morningAnswerJson)
                .eveningAnswers(eveningAnswersJson)
                .morningAnswered(morningAnswered)
                .eveningCompletedCount(eveningCompletedCount)
                .eveningTotalCount(eveningTotalCount)
                .build();

        dailyLogRepository.save(dailyLog);
    }

    public DailyLogResponse getDailyLog(Long userId, LocalDate date) {

        // 1. dailyLogRepository로 그날의 DailyLog 조회 (없으면 어떻게 할지도 고민)
        Optional<DailyLog> dailyLogOpt = dailyLogRepository.findByUserIdAndLogDate(userId, date);
        if (dailyLogOpt.isEmpty()) {
            DailyLogResponse response = DailyLogResponse.builder()
                    .logDate(date.toString())
                    .morningAnswer(null)
                    .eveningAnswers(new ArrayList<>())
                    .morningAnswered(false)
                    .eveningCompletedCount(0)
                    .eveningTotalCount(0)
                    .build();

            return response;
        } else {
            DailyLog dailyLog = dailyLogOpt.get();

            String logDate = dailyLog.getLogDate().toString();
            boolean morningAnswered = dailyLog.isMorningAnswered();
            int eveningCompletedCount = dailyLog.getEveningCompletedCount();
            int eveningTotalCount = dailyLog.getEveningTotalCount();

            String morningAnswerJson = dailyLog.getMorningAnswer();
            String eveningAnswersJson = dailyLog.getEveningAnswers();

            DailyLogResponse.MorningAnswerItem morningAnswerItem;
            List<DailyLogResponse.EveningAnswerItem> eveningAnswerItems;

            try {
                morningAnswerItem = morningAnswerJson != null
                        ? objectMapper.readValue(morningAnswerJson, DailyLogResponse.MorningAnswerItem.class)
                        : null;
                eveningAnswerItems = objectMapper.readValue(eveningAnswersJson,
                        new TypeReference<List<DailyLogResponse.EveningAnswerItem>>() {});
            } catch (JacksonException e) {
                throw new RuntimeException("DailyLog 파싱 실패", e);
            }

            return DailyLogResponse.builder()
                    .logDate(logDate)
                    .morningAnswered(morningAnswered)
                    .eveningCompletedCount(eveningCompletedCount)
                    .eveningTotalCount(eveningTotalCount)
                    .morningAnswer(morningAnswerItem)
                    .eveningAnswers(eveningAnswerItems)
                    .build();
        }
    }
}
