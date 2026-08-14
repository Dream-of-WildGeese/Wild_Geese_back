package com.ondam.report.service;

import com.ondam.global.common.DateUtils;
import com.ondam.report.entity.WeeklyReport;
import com.ondam.report.repository.WeeklyReportRepository;
import com.ondam.report.dto.response.WeeklyReportResponse;
import com.ondam.question.entity.MetricType;
import com.ondam.record.repository.HealthRecordRepository;
import com.ondam.record.entity.HealthRecord;
import com.ondam.medication.repository.MedicationLogRepository;
import com.ondam.medication.entity.MedicationLogStatus;

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
    private final ObjectMapper objectMapper;

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
                MetricType.MEAL, MetricType.ACTIVITY, MetricType.CUSTOM);

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

            // e. comment (고정 문구, 다음 단계에서)
            String comment;
            if (trend.equals("DECREASE")) {
                comment = "지난주보다 조금 낮아졌어요.";
            } else if (trend.equals("INCREASE")) {
                comment = "지난주보다 나아졌어요.";
            } else if (trend.equals("SAME")) {
                comment = "지난주와 비슷했어요.";
            } else {
                comment = "비교할 데이터가 부족해요.";
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
                    .comment(comment)
                    .build();

            metrics.put(type.toString(), detail);
        }
        // 5. weeklyComment, nextWeekSuggestion도 고정 문구로
        String weeklyComment = isBaselineSufficient
                ? "이번 주 건강 기록을 확인해보세요."
                : "비교할 지난주 기록이 부족해요.";

        String nextWeekSuggestion = "다음 주에도 꾸준히 기록해보시는 건 어때요?";
        // AI연동 필요

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
                .metrics(metrics)
                .medication(medicationSummary)
                .nextWeekSuggestion(nextWeekSuggestion)
                .build();
    }
}