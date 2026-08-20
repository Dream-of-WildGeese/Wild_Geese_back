package com.ondam.report.service;

import com.ondam.global.common.DateUtils;
import com.ondam.global.util.GptClient;
import com.ondam.report.entity.WeeklyReport;
import com.ondam.report.repository.WeeklyReportRepository;
import com.ondam.report.dto.response.WeeklyReportResponse;
import com.ondam.report.dto.response.WeeklyReportHistoryItem;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.HashMap;

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

    @Transactional
    public WeeklyReportResponse getWeeklyReport(Long userId, LocalDate weekStartDate) {

        // 1. 이번 주 월요일~일요일 날짜 계산
        LocalDate thisWeekStart;
        if (weekStartDate != null) {
            thisWeekStart = weekStartDate.with(DayOfWeek.MONDAY);   // 55번째 줄 수정
        } else {
            LocalDate today = DateUtils.today();
            thisWeekStart = today.with(DayOfWeek.MONDAY);
        }

        LocalDate thisWeekEnd = thisWeekStart.plusDays(6);

        LocalDate today = DateUtils.today();
        LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);
        boolean isPastWeek = thisWeekStart.isBefore(currentWeekStart);

        if (isPastWeek) {
            Optional<WeeklyReport> existingOpt = weeklyReportRepository.findByUserIdAndWeekStartDate(userId, thisWeekStart);
            if (existingOpt.isPresent()) {
                return convertToResponse(existingOpt.get());
            }
        }

        // 2. 지난 주 월요일~일요일 날짜 계산
        LocalDate lastWeekStart = thisWeekStart.minusDays(7);
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

        // 3. 5개 MetricType(CONDITION, SLEEP, MEAL, ACTIVITY, CUSTOM)마다 반복:
        Map<String, WeeklyReportResponse.MetricDetail> metrics = new HashMap<>();
        Map<String, List<Double>> lastWeekDailyMap = new HashMap<>();
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

            // 프롬프트용 - 지난주 요일별 값 조회 (API 응답 DTO에는 포함 안 함)
            List<HealthRecord> lastWeekRecords = healthRecordRepository
                    .findByUserIdAndMetricTypeAndRecordDateBetween(userId, type, lastWeekStart, lastWeekEnd);

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

            // 지난주 요일별 값도 저장 (프롬프트용)
            List<Double> lastWeekDailyValues = new ArrayList<>();
            for (HealthRecord r : lastWeekRecords) {
                if (r.getNumericValue() != null) {
                    lastWeekDailyValues.add(r.getNumericValue().doubleValue());
                }
            }
            lastWeekDailyMap.put(type.toString(), lastWeekDailyValues);

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

        List<EveningQuestion> customQuestions = eveningQuestionRepository
                .findByUserIdAndMetricTypeAndQuestionDateBetween(
                        userId, MetricType.CUSTOM, thisWeekStart, thisWeekEnd);
        List<String> customTexts = new ArrayList<>();

        for (EveningQuestion cq : customQuestions) {
            Optional<EveningAnswer> answerOpt = eveningAnswerRepository
                    .findFirstByEveningQuestionIdAndUserId(cq.getId(), userId);
            if (answerOpt.isPresent() && answerOpt.get().getTextValue() != null) {
                customTexts.add(answerOpt.get().getTextValue());
            }
        }
        Map<String, String> aiComments = generateAllComments(metrics, customTexts, lastWeekDailyMap);

        Map<String, WeeklyReportResponse.MetricDetail> finalMetrics = new HashMap<>();

        for (Map.Entry<String, WeeklyReportResponse.MetricDetail> entry : metrics.entrySet()) {
            WeeklyReportResponse.MetricDetail old = entry.getValue();
            String commentKey = entry.getKey().toLowerCase() + "Comment";

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

        String weeklyComment = aiComments.getOrDefault("weeklyComment", "평범하게 지낸 주");
        String nextWeekSuggestion = aiComments.getOrDefault("nextWeekSuggestion", "다음 주에도 꾸준히 기록해보시는 건 어때요?");
        String customComment = aiComments.getOrDefault("customComment", "이번 주 질환 관련 답변을 확인했어요.");

        // metrics를 JSON 문자열로 변환
        String metricsJson;
        try {
            metricsJson = objectMapper.writeValueAsString(finalMetrics);
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
                .weeklyDetail(aiComments.getOrDefault("weeklyDetail", "이번 주 건강 흐름을 확인해보세요."))
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
                .weeklyDetail(aiComments.getOrDefault("weeklyDetail", "이번 주 건강 흐름을 확인해보세요."))
                .metrics(finalMetrics)
                .medication(medicationSummary)
                .customComment(customComment)
                .nextWeekSuggestion(nextWeekSuggestion)
                .build();
    }

    private String translateMetricType(String key) {
        return switch (key) {
            case "CONDITION" -> "컨디션";
            case "SLEEP" -> "수면";
            case "MEAL" -> "식사";
            case "ACTIVITY" -> "활동";
            default -> key;
        };
    }

    private String buildMetricsDescription(
            Map<String, WeeklyReportResponse.MetricDetail> metrics,
            Map<String, List<Double>> lastWeekDailyMap) {

        StringBuilder sb = new StringBuilder();
        String[] dayLabels = {"월", "화", "수", "목", "금", "토", "일"};

        for (Map.Entry<String, WeeklyReportResponse.MetricDetail> entry : metrics.entrySet()) {
            WeeklyReportResponse.MetricDetail detail = entry.getValue();
            String key = entry.getKey();
            String koreanName = translateMetricType(key);

            sb.append(key).append(": ");
            sb.append("이번주 평균 ").append(detail.current());
            sb.append(", 지난주 평균 ").append(detail.previous());
            sb.append(", 추세 ").append(detail.trend());

            List<Double> thisWeekDaily = detail.daily();
            if (thisWeekDaily != null && !thisWeekDaily.isEmpty()) {
                sb.append(", 이번주 요일별(월~일 순): ");
                for (int i = 0; i < thisWeekDaily.size() && i < 7; i++) {
                    sb.append(dayLabels[i]).append("=").append(thisWeekDaily.get(i)).append(" ");
                }
            }

            List<Double> lastWeekDaily = lastWeekDailyMap.get(key);
            if (lastWeekDaily != null && !lastWeekDaily.isEmpty()) {
                sb.append(", 지난주 요일별(월~일 순): ");
                for (int i = 0; i < lastWeekDaily.size() && i < 7; i++) {
                    sb.append(dayLabels[i]).append("=").append(lastWeekDaily.get(i)).append(" ");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private Map<String, String> generateAllComments(
            Map<String, WeeklyReportResponse.MetricDetail> metrics,
            List<String> customTexts,
            Map<String, List<Double>> lastWeekDailyMap) {

        try {
            String metricsDesc = buildMetricsDescription(metrics, lastWeekDailyMap);
            String customDesc = customTexts.isEmpty() ? "없음" : String.join(" / ", customTexts);

            String prompt = String.format("""
            당신은 어르신 건강 관리 앱의 리포트 작성자입니다.
            이 리포트는 어르신 본인과 그 자녀가 함께 봅니다.
    
            이번 주 건강 데이터 (점수는 1~3점, 높을수록 좋은 상태):
            %s
    
            지병 관련 답변: %s
    
            아래 JSON 형식으로만 답변하세요. 다른 설명 없이 JSON만 출력하세요.
            {
              "weeklyComment": "이번 주 전체 흐름을 요약하는 짧은 제목. 반드시 '~한 주', '~주' 형태의 명사형으로 끝내세요. (예: '잠이 부족한 주', '잘 챙겨 먹은 주', '활동량 최고!', '바쁜 한 주')",
              "weeklyDetail": "weeklyComment를 뒷받침하는 두 문장. 여러 지표를 연결해서 설명하고, 잘 지킨 부분이 있으면 짧게 격려하세요. 마지막 문장은 다음 주 제안이나 안부를 건네는 부드러운 문장으로 마무리하세요.",
              "conditionComment": "다음 두 가지를 각각 한 문장씩, 총 두 문장으로 작성하세요. 1) 이번 주 요일별 데이터를 보고, 낮았던 날이 있으면 정확한 요일(또는 요일 범위)을 짚어 설명하세요. '점수가 낮았어요' 대신 '컨디션이 안 좋으셨어요'처럼 상태를 표현하는 말로 쓰세요. 낮은 날이 없으면 '~요일 모두 좋은 컨디션을 유지하셨어요'처럼 칭찬하세요. 2) 이번주 평균이 2.5 이상이면 '대체로 양호했어요' 같은 긍정적 언급을 먼저 하고, 지난주 대비 변화를 덧붙이세요. 이번주 평균이 2.5 미만이면 지난주 대비를 '좋아졌어요', '비슷해요', '나빠졌어요' 중 하나로 짧게 표현하세요. 절대로 숫자나 소수점 수치를 언급하지 마세요.",
              "sleepComment": "다음 두 가지를 각각 한 문장씩, 총 두 문장으로 작성하세요. 1) 이번 주 요일별 데이터를 보고, 부족했던 날이 있으면 정확한 요일을 짚어 설명하세요. '푹 주무셨어요', '잠이 부족하셨어요'처럼 상태를 표현하는 말로 쓰세요. 부족한 날이 없으면 '~요일 모두 충분히 주무셨어요'처럼 칭찬하세요. 2) 이번주 평균이 2.5 이상이면 '대체로 수면은 양호했어요' 같은 긍정적 언급을 먼저 하고, 지난주 대비 변화를 덧붙이세요. 이번주 평균이 2.5 미만이면 지난주 대비를 '좋아졌어요', '비슷해요', '나빠졌어요' 중 하나로 짧게 표현하세요. 절대로 숫자나 소수점 수치를 언급하지 마세요.",
              "mealComment": "다음 두 가지를 각각 한 문장씩, 총 두 문장으로 작성하세요. 1) 이번 주 요일별 데이터를 보고, 부족했던 날이 있으면 정확한 요일을 짚어 설명하세요. '잘 챙기셨어요', '많이 거르셨어요'처럼 상태를 표현하는 말로 쓰세요. 부족한 날이 없으면 '~요일 모두 식사를 잘 챙기셨어요'처럼 칭찬하세요. 2) 이번주 평균이 2.5 이상이면 '대체로 식사는 양호했어요' 같은 긍정적 언급을 먼저 하고, 지난주 대비 변화를 덧붙이세요. 이번주 평균이 2.5 미만이면 지난주 대비를 '좋아졌어요', '비슷해요', '나빠졌어요' 중 하나로 짧게 표현하세요. 절대로 숫자나 소수점 수치를 언급하지 마세요.",
              "activityComment": "다음 두 가지를 각각 한 문장씩, 총 두 문장으로 작성하세요. 1) 이번 주 요일별 데이터를 보고, 적었던 날이 있으면 정확한 요일을 짚어 설명하세요. '활동량이 많으셨어요', '많이 못 움직이셨어요'처럼 상태를 표현하는 말로 쓰세요. 적은 날이 없으면 '~요일 모두 활발하게 움직이셨어요'처럼 칭찬하세요. 2) 이번주 평균이 2.5 이상이면 '대체로 활동량은 양호했어요' 같은 긍정적 언급을 먼저 하고, 지난주 대비 변화를 덧붙이세요. 이번주 평균이 2.5 미만이면 지난주 대비를 '좋아졌어요', '비슷해요', '나빠졌어요' 중 하나로 짧게 표현하세요. 절대로 숫자나 소수점 수치를 언급하지 마세요.",
              "customComment": "지병 관련 답변을 요약한 한 줄 코멘트",
              "nextWeekSuggestion": "다음 주를 위한 부드러운 제안 한 문장. 명령이 아니라 제안하는 어투로"
            }
    
            conditionComment 예시: "특히 목요일과 금요일에 컨디션이 안 좋으셨어요. 지난주와 비교해 컨디션은 비슷한 수준이에요."
            
            문체 규칙:
            - 모든 문장은 존댓말로, 부드럽고 다정한 어투로 작성하세요.
            - weeklyComment는 15자 내외의 짧은 제목, weeklyDetail은 두 문장 이내로 작성하세요.
            - weeklyDetail을 포함한 모든 문장에서 지표 이름은 반드시 한글(컨디션/수면/식사/활동)로만 쓰세요. 영어 단어(SLEEP, MEAL 등)를 절대 쓰지 마세요.
            - 지표별 코멘트(conditionComment~activityComment)는 반드시 두 문장으로, 요일별 데이터에 실제로 나타나는 값만 근거로 사용하세요. 데이터에 없는 요일을 지어내지 마세요.
            - 지표별 코멘트 작성 시, "이번주 평균"이 2.5 이상이면 절대적으로는 양호한 상태임을 먼저 언급하고, 그 뒤에 지난주 대비 변화를 덧붙이세요. (예: "이번 주도 활동량은 양호한 편이었어요. 다만 지난주보다는 조금 줄었어요.") "이번주 평균"이 2.5 미만이면, 지난주 대비 여부와 상관없이 현재 상태를 그대로 표현하세요.
            - "점수", "스코어", 소수점 수치, 정확한 숫자를 절대 언급하지 마세요. 대신 '좋았어요', '부족했어요', '늘었어요', '줄었어요' 같은 자연스러운 표현만 쓰세요.
            - 절대로 의학적 진단을 내리거나, "때문에" 같은 인과관계를 단정짓지 마세요.
            - 나쁜 수치가 있어도 걱정을 유발하지 말고, 담담하고 따뜻하게 전달하세요.
            """, metricsDesc, customDesc);

            String response = gptClient.ask(prompt);

            String cleaned = response.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(cleaned, new TypeReference<Map<String, String>>() {});

        } catch (Exception e) {
            // 실패 시 기본 문구로 폴백
            Map<String, String> fallback = new HashMap<>();
            fallback.put("weeklyComment", "평범하게 지낸 주");
            fallback.put("weeklyDetail", "이번 주 건강 흐름을 확인해보세요.");
            fallback.put("nextWeekSuggestion", "다음 주에도 꾸준히 기록해보시는 건 어때요?");
            fallback.put("customComment", "이번 주 질환 관련 답변을 확인했어요.");
            return fallback;
        }
    }

    public List<WeeklyReportHistoryItem> getWeeklyReportHistory(Long userId) {

        List<WeeklyReport> reports = weeklyReportRepository.findByUserIdOrderByWeekStartDateDesc(userId);

        List<WeeklyReportHistoryItem> result = new ArrayList<>();

        for (WeeklyReport report : reports) {
            WeeklyReportHistoryItem item = WeeklyReportHistoryItem.builder()
                    .weekStartDate(report.getWeekStartDate().toString())
                    .weekEndDate(report.getWeekEndDate().toString())
                    .weeklyComment(report.getAiSummary())
                    .build();

            result.add(item);
        }

        return result;
    }

    private WeeklyReportResponse convertToResponse(WeeklyReport report) {
        try {
            Map<String, WeeklyReportResponse.MetricDetail> metrics = objectMapper.readValue(
                    report.getMetricsSummary(),
                    new TypeReference<Map<String, WeeklyReportResponse.MetricDetail>>() {});

            // 복약 정보는 과거 주차라도 그때 그대로 다시 계산 (복약 로그 자체는 안 바뀌니 문제없음)
            long takenCount = medicationLogRepository.countByUserIdAndStatusAndRecordDateBetween(
                    report.getUserId(), MedicationLogStatus.TAKEN, report.getWeekStartDate(), report.getWeekEndDate());
            long totalCount = medicationLogRepository.countByUserIdAndStatusAndRecordDateBetween(
                    report.getUserId(), MedicationLogStatus.NOT_RECORDED, report.getWeekStartDate(), report.getWeekEndDate()) + takenCount;

            WeeklyReportResponse.MedicationSummary medicationSummary = WeeklyReportResponse.MedicationSummary.builder()
                    .takenCount(takenCount)
                    .totalCount(totalCount)
                    .comment("이번 주 " + totalCount + "번 중 " + takenCount + "번 챙기셨어요.")
                    .build();

            return WeeklyReportResponse.builder()
                    .weekStartDate(report.getWeekStartDate().toString())
                    .weekEndDate(report.getWeekEndDate().toString())
                    .isBaselineSufficient(report.isBaselineSufficient())
                    .weeklyComment(report.getAiSummary())
                    .weeklyDetail(report.getWeeklyDetail())
                    .metrics(metrics)
                    .medication(medicationSummary)
                    .nextWeekSuggestion("")   // 별도 저장 컬럼 없으면 빈 값
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("과거 리포트 변환 실패", e);
        }
    }
}
