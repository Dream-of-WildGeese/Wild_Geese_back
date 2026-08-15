package com.ondam.report.service;

import com.ondam.global.common.DateUtils;
import com.ondam.global.util.GptClient;
import com.ondam.report.entity.WeeklyReport;
import com.ondam.report.repository.WeeklyReportRepository;
import com.ondam.report.dto.response.WeeklyReportResponse;
import com.ondam.question.entity.MetricType;
import com.ondam.record.repository.HealthRecordRepository;
import com.ondam.record.entity.HealthRecord;
import com.ondam.medication.repository.MedicationLogRepository;
import com.ondam.medication.entity.MedicationLogStatus;
import com.ondam.question.entity.EveningQuestion;
import com.ondam.question.entity.EveningAnswer;
import com.ondam.question.repository.EveningQuestionRepository;
import com.ondam.question.repository.EveningAnswerRepository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final MedicationLogRepository medicationLogRepository;
    private final EveningQuestionRepository eveningQuestionRepository;
    private final EveningAnswerRepository eveningAnswerRepository;
    private final ObjectMapper objectMapper;
    private final GptClient gptClient;

    public WeeklyReportResponse getWeeklyReport(Long userId, LocalDate weekStartDate) {

        // 1. 이번 주 월요일~일요일 날짜 계산
        LocalDate thisWeekStart;
        if (weekStartDate != null) {
            thisWeekStart = weekStartDate;
        } else {
            LocalDate today = DateUtils.today();
            thisWeekStart = today.with(DayOfWeek.MONDAY);
        }

        LocalDate thisWeekEnd = thisWeekStart.plusDays(6);

        // 2. 지난 주 월요일~일요일 날짜 계산
        LocalDate lastWeekStart = thisWeekStart.minusDays(7);
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

        // 3. 5개 MetricType(CONDITION, SLEEP, MEAL, ACTIVITY, CUSTOM)마다 반복:
        Map<String, WeeklyReportResponse.MetricDetail> metrics = new HashMap<>();
        boolean isBaselineSufficient = false;

        List<MetricType> types = List.of(MetricType.CONDITION, MetricType.SLEEP,
                MetricType.MEAL, MetricType.ACTIVITY);

        for (MetricType type : types) {

            // a. 이번주 평균
            BigDecimal thisWeekAvg = healthRecordRepository.findAverageValue(userId, type, thisWeekStart, thisWeekEnd);

            // b. 지난주 평균
            BigDecimal lastWeekAvg = healthRecordRepository.findAverageValue(userId, type, lastWeekStart, lastWeekEnd);

            // c. 이번주 일별 리스트
            List<HealthRecord> thisWeekRecords = healthRecordRepository
                    .findByUserIdAndMetricTypeAndRecordDateBetween(userId, type, thisWeekStart, thisWeekEnd);

            // d. diff, trend 계산 (다음 단계에서)
            Double diff = null;
            String trend = "UNKNOWN";   // 비교할 지난주 데이터가 없을 때

            if (lastWeekAvg != null && thisWeekAvg != null) {
                diff = thisWeekAvg.doubleValue() - lastWeekAvg.doubleValue();
                isBaselineSufficient = true;

                if (diff > 0) {
                    trend = "INCREASE";
                } else if (diff < 0) {
                    trend = "DECREASE";
                } else {
                    trend = "SAME";
                }
            }

            // f. Map에 담기
            List<Double> dailyValues = new ArrayList<>();
            for (HealthRecord r : thisWeekRecords) {
                if (r.getNumericValue() != null) {
                    dailyValues.add(r.getNumericValue().doubleValue());
                }
            }

            WeeklyReportResponse.MetricDetail detail = WeeklyReportResponse.MetricDetail.builder()
                    .current(thisWeekAvg != null ? thisWeekAvg.doubleValue() : null)
                    .previous(lastWeekAvg != null ? lastWeekAvg.doubleValue() : null)
                    .diff(diff)
                    .trend(trend)
                    .daily(dailyValues)
                    .comment(null)
                    .build();

            metrics.put(type.toString(), detail);
        }

        // CUSTOM 답변들을 모아서 코멘트 생성 (나중에 GPT 연동)
        List<EveningQuestion> customQuestions = eveningQuestionRepository
                .findByUserIdAndMetricTypeAndQuestionDateBetween(
                        userId, MetricType.CUSTOM, thisWeekStart, thisWeekEnd);
        List<String> customTexts = new ArrayList<>();

        Map<String, String> aiComments = generateAllComments(metrics, customTexts);

        for (EveningQuestion cq : customQuestions) {
            Optional<EveningAnswer> answerOpt = eveningAnswerRepository
                    .findByEveningQuestionIdAndUserId(cq.getId(), userId);
            if (answerOpt.isPresent() && answerOpt.get().getTextValue() != null) {
                customTexts.add(answerOpt.get().getTextValue());
            }
        }

        Map<String, WeeklyReportResponse.MetricDetail> finalMetrics = new HashMap<>();
        for (Map.Entry<String, WeeklyReportResponse.MetricDetail> entry : metrics.entrySet()) {
            WeeklyReportResponse.MetricDetail old = entry.getValue();
            String commentKey = entry.getKey().toLowerCase() + "Comment";  // 예: "conditionComment"

            WeeklyReportResponse.MetricDetail updated = WeeklyReportResponse.MetricDetail.builder()
                    .current(old.current())
                    .previous(old.previous())
                    .diff(old.diff())
                    .trend(old.trend())
                    .daily(old.daily())
                    .comment(aiComments.getOrDefault(commentKey, "데이터를 확인해보세요."))
                    .build();

            finalMetrics.put(entry.getKey(), updated);
        }

        String weeklyComment = aiComments.getOrDefault("weeklyComment", "이번 주 건강 기록을 확인해보세요.");
        String nextWeekSuggestion = aiComments.getOrDefault("nextWeekSuggestion", "다음 주에도 꾸준히 기록해보시는 건 어때요?");
        String customComment = aiComments.getOrDefault("customComment", "이번 주 질환 관련 답변을 확인했어요.");

        // metrics를 JSON 문자열로 변환
        String metricsJson;
        try {
            metricsJson = objectMapper.writeValueAsString(metrics);
        } catch (JacksonException e) {
            throw new RuntimeException("WeeklyReport JSON 변환 실패", e);
        }

        // 기존에 이번 주 리포트가 있으면 지우고 새로 저장 (DailyLog에서 했던 방식과 동일)
        Optional<WeeklyReport> existingOpt = weeklyReportRepository.findByUserIdAndWeekStartDate(userId, thisWeekStart);
        if (existingOpt.isPresent()) {
            weeklyReportRepository.delete(existingOpt.get());
        }

        WeeklyReport weeklyReport = WeeklyReport.builder()
                .userId(userId)
                .weekStartDate(thisWeekStart)
                .weekEndDate(thisWeekEnd)
                .metricsSummary(metricsJson)
                .aiSummary(weeklyComment)
                .isBaselineSufficient(isBaselineSufficient)
                .build();

        weeklyReportRepository.save(weeklyReport);

        long takenCount = medicationLogRepository.countByUserIdAndStatusAndRecordDateBetween(
                userId, MedicationLogStatus.TAKEN, thisWeekStart, thisWeekEnd);

        long totalCount = medicationLogRepository.countByUserIdAndStatusAndRecordDateBetween(
                userId, MedicationLogStatus.NOT_RECORDED, thisWeekStart, thisWeekEnd) + takenCount;

        WeeklyReportResponse.MedicationSummary medicationSummary = WeeklyReportResponse.MedicationSummary.builder()
                .takenCount(takenCount)
                .totalCount(totalCount)
                .comment("이번 주 " + totalCount + "번 중 " + takenCount + "번 챙기셨어요.")
                .build();

        // 6. DTO 조립
        return WeeklyReportResponse.builder()
                .weekStartDate(thisWeekStart.toString())
                .weekEndDate(thisWeekEnd.toString())
                .isBaselineSufficient(isBaselineSufficient)
                .weeklyComment(weeklyComment)
                .metrics(finalMetrics)
                .medication(medicationSummary)
                .customComment(customComment)
                .nextWeekSuggestion(nextWeekSuggestion)
                .build();
    }

    private String buildMetricsDescription(Map<String, WeeklyReportResponse.MetricDetail> metrics) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, WeeklyReportResponse.MetricDetail> entry : metrics.entrySet()) {
            WeeklyReportResponse.MetricDetail detail = entry.getValue();
            sb.append(entry.getKey())
                    .append(": 이번주 ").append(detail.current())
                    .append(", 지난주 ").append(detail.previous())
                    .append(", 추세 ").append(detail.trend())
                    .append("\n");
        }
        return sb.toString();
    }

    private Map<String, String> generateAllComments(
            Map<String, WeeklyReportResponse.MetricDetail> metrics,
            List<String> customTexts) {

        try {
            String metricsDesc = buildMetricsDescription(metrics);
            String customDesc = customTexts.isEmpty() ? "없음" : String.join(" / ", customTexts);

            String prompt = String.format("""
                당신은 어르신 건강 관리 앱의 리포트 작성자입니다.
                다음은 이번 주 건강 데이터입니다:
                %s
                질환 관련 답변: %s
                
                아래 JSON 형식으로만 답변하세요. 다른 설명 없이 JSON만 출력하세요.
                {
                  "weeklyComment": "이번 주 전체를 한 줄로 요약한 따뜻한 문장",
                  "conditionComment": "컨디션 지표에 대한 한 줄 코멘트",
                  "sleepComment": "수면 지표에 대한 한 줄 코멘트",
                  "mealComment": "식사 지표에 대한 한 줄 코멘트",
                  "activityComment": "활동 지표에 대한 한 줄 코멘트",
                  "customComment": "질환 관련 답변을 요약한 한 줄 코멘트",
                  "nextWeekSuggestion": "다음 주를 위한 부드러운 제안 한 문장"
                }
                
                부정적인 진단이나 인과관계 추측은 하지 마세요.
                """, metricsDesc, customDesc);

            String response = gptClient.ask(prompt);

            // GPT가 가끔 ```json ... ``` 코드블럭으로 감싸서 응답하는 경우 제거
            String cleaned = response.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(cleaned, new TypeReference<Map<String, String>>() {});

        } catch (Exception e) {
            // 실패 시 기본 문구로 폴백
            Map<String, String> fallback = new HashMap<>();
            fallback.put("weeklyComment", "이번 주 건강 기록을 확인해보세요.");
            fallback.put("nextWeekSuggestion", "다음 주에도 꾸준히 기록해보시는 건 어때요?");
            fallback.put("customComment", "이번 주 질환 관련 답변을 확인했어요.");
            return fallback;
        }
    }
}