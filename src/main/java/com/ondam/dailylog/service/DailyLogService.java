package com.ondam.dailylog.service;

import com.ondam.global.util.GptClient;
import com.ondam.dailylog.entity.DailyLog;
import com.ondam.dailylog.repository.DailyLogRepository;
import com.ondam.dailylog.dto.response.DailyLogResponse;
import com.ondam.dailylog.dto.response.DailyLogSummaryItem;
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
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class DailyLogService {

    private final DailyLogRepository dailyLogRepository;
    private final MorningAnswerRepository morningAnswerRepository;
    private final MorningQuestionRepository morningQuestionRepository;
    private final EveningAnswerRepository eveningAnswerRepository;
    private final EveningQuestionRepository eveningQuestionRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final GptClient gptClient;

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
                    .findFirstByMorningQuestionIdAndUserId(questionOpt.get().getId(), userId);            if(answerOpt.isPresent()){
                morningAnswer = answerOpt.get();
            }
        }

        List<EveningQuestion> eveningQuestions = eveningQuestionRepository
                .findByUserIdAndQuestionDate(userId, date);

        List<Map<String, Object>> eveningAnswers = new ArrayList<>();

        for (EveningQuestion eq : eveningQuestions) {
            Optional<EveningAnswer> answerOpt = eveningAnswerRepository
                    .findFirstByEveningQuestionIdAndUserId(eq.getId(), userId);
            if (answerOpt.isPresent()) {
                EveningAnswer ea = answerOpt.get();
                Map<String, Object> item = new HashMap<>();
                item.put("metricType", eq.getMetricType().toString());
                item.put("textValue", ea.getTextValue());
                item.put("choiceValue", ea.getChoiceValue());
                item.put("answeredAt", ea.getAnsweredAt());
                eveningAnswers.add(item);
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

        String summaryText = generateDailySummary(morningAnswerJson, eveningAnswers, eveningCompletedCount, eveningTotalCount);
        Optional<DailyLog> existingLogOpt = dailyLogRepository.findByUserIdAndLogDate(userId, date);

        if (existingLogOpt.isPresent()) {
            // 이미 있으면 → 기존 걸 지우고 새로 만들어서 저장
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
                .summaryText(summaryText)
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
                    .summaryText("아직 오늘 기록이 없어요.")
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
                    .summaryText(dailyLog.getSummaryText())
                    .build();
        }
    }

    public List<DailyLogSummaryItem> getDailySummary(Long userId, LocalDate from, LocalDate to) {

        // 1. dailyLogRepository로 범위 조회 → List<DailyLog>
        List<DailyLog> bound = dailyLogRepository.findByUserIdAndLogDateBetween(userId, from, to);

        List<DailyLogSummaryItem> result = new ArrayList<>();

        for(DailyLog log : bound){
            DailyLogSummaryItem item = DailyLogSummaryItem.builder()
                    .logDate(log.getLogDate().toString())
                    .morningAnswered(log.isMorningAnswered())
                    .eveningCompletedCount(log.getEveningCompletedCount())
                    .eveningTotalCount(log.getEveningTotalCount())
                    .build();

            result.add(item);
        }

        return result;
    }

    private String generateDailySummary(String morningAnswerJson, List<Map<String, Object>> eveningAnswers,
                                        int eveningCompletedCount, int eveningTotalCount) {

        if (eveningCompletedCount == 0 && morningAnswerJson == null) {
            return "아직 오늘 기록이 없어요.";
        }

        try {
            StringBuilder dataText = new StringBuilder();
            dataText.append("저녁 체크 완료: ").append(eveningCompletedCount).append("/").append(eveningTotalCount).append("\n");

            for (Map<String, Object> ans : eveningAnswers) {
                dataText.append(ans.get("metricType")).append(": ");
                if (ans.get("textValue") != null) {
                    dataText.append(ans.get("textValue"));
                }
                dataText.append("\n");
            }

            String prompt = String.format("""
                당신은 어르신 건강 관리 앱의 일일 요약 작성자입니다.
                오늘 하루의 건강 기록입니다:
                %s

                이 기록을 보고, 오늘 하루를 한 문장으로 따뜻하게 요약해주세요.

                규칙:
                - 반드시 한 문장, 15~25자 내외로 작성하세요.
                - 숫자나 점수를 언급하지 말고, "잘 챙기셨어요", "조금 힘든 하루였어요"처럼 자연스러운 말로 표현하세요.
                - 기록이 일부만 있으면 그 사실 자체를 담담하게 표현하세요.
                - 의학적 진단이나 걱정을 유발하는 표현은 쓰지 마세요.
                - 문장만 출력하고 다른 설명은 하지 마세요.
                """, dataText.toString());

            return gptClient.ask(prompt).trim();

        } catch (Exception e) {
            return "오늘도 기록해주셔서 감사해요.";
        }
    }
}
