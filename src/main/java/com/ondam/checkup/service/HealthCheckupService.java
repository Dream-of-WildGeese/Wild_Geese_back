package com.ondam.checkup.service;

import com.ondam.checkup.dto.request.HealthCheckupRequest;
import com.ondam.checkup.dto.response.HealthCheckupResponse;
import com.ondam.checkup.entity.HealthCheckup;
import com.ondam.checkup.repository.HealthCheckupRepository;
import com.ondam.global.common.DateUtils;
import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
import com.ondam.global.util.GptClient;
import com.ondam.question.entity.EveningAnswer;
import com.ondam.question.entity.EveningQuestion;
import com.ondam.question.entity.MetricType;
import com.ondam.question.repository.EveningAnswerRepository;
import com.ondam.question.repository.EveningQuestionRepository;
import com.ondam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthCheckupService {

    private final HealthCheckupRepository healthCheckupRepository;
    private final UserRepository userRepository;
    private final EveningQuestionRepository eveningQuestionRepository;
    private final EveningAnswerRepository eveningAnswerRepository;
    private final GptClient gptClient;

    @Transactional
    public Long createCheckup(Long userId, HealthCheckupRequest request) {
        validateUser(userId);

        HealthCheckup checkup = HealthCheckup.builder()
                .userId(userId)
                .checkupDate(request.checkupDate())
                .checkupType(request.checkupType())
                .hospitalName(request.hospitalName())
                .isCompleted(false)
                .build();

        return healthCheckupRepository.save(checkup).getId();
    }

    public HealthCheckupResponse getCheckupScreen(Long userId) {
        validateUser(userId);
        LocalDate today = DateUtils.today();

        // 1. 다가오는 가장 빠른 검진
        Optional<HealthCheckup> upcomingOpt = healthCheckupRepository
                .findFirstByUserIdAndCheckupDateGreaterThanEqualAndIsCompletedFalseOrderByCheckupDateAsc(userId, today);

        HealthCheckupResponse.UpcomingCheckupItem upcomingItem = upcomingOpt.map(c -> {
            long dDay = ChronoUnit.DAYS.between(today, c.getCheckupDate());
            return HealthCheckupResponse.UpcomingCheckupItem.builder()
                    .checkupId(c.getId())
                    .checkupDate(c.getCheckupDate())
                    .checkupType(c.getCheckupType())
                    .hospitalName(c.getHospitalName())
                    .dDay(dDay)
                    .build();
        }).orElse(null);

        // 2. 지난 검진 이력 목록
        List<HealthCheckup> pastCheckups = healthCheckupRepository
                .findByUserIdAndCheckupDateBeforeOrderByCheckupDateDesc(userId, today);

        List<HealthCheckupResponse.PastCheckupItem> pastItems = pastCheckups.stream()
                .map(c -> HealthCheckupResponse.PastCheckupItem.builder()
                        .checkupId(c.getId())
                        .checkupDate(c.getCheckupDate())
                        .checkupType(c.getCheckupType())
                        .hospitalName(c.getHospitalName())
                        .relativeTime(formatRelativeTime(c.getCheckupDate(), today))
                        .build())
                .toList();

        // 3. 최근 30일간 저녁 CUSTOM 기록 기반 "진료 질문/증상 변화" 추출
        List<String> doctorQuestions = generateDoctorQuestions(userId, today);

        return HealthCheckupResponse.builder()
                .upcomingCheckup(upcomingItem)
                .pastCheckups(pastItems)
                .doctorQuestions(doctorQuestions)
                .build();
    }

    @Transactional
    public void updateCheckup(Long userId, Long checkupId, HealthCheckupRequest request) {
        HealthCheckup checkup = healthCheckupRepository.findByIdAndUserId(checkupId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKUP_NOT_FOUND));

        // isCompleted는 null로 넘겨서 기존 상태를 유지하게 함
        checkup.update(request.checkupDate(), request.checkupType(), request.hospitalName(), null);
    }

    @Transactional
    public void deleteCheckup(Long userId, Long checkupId) {
        HealthCheckup checkup = healthCheckupRepository.findByIdAndUserId(checkupId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKUP_NOT_FOUND)); // USER_NOT_FOUND -> CHECKUP_NOT_FOUND 변경!
        healthCheckupRepository.delete(checkup);
    }

    private List<String> generateDoctorQuestions(Long userId, LocalDate today) {
        LocalDate startDate = today.minusDays(30);

        List<EveningQuestion> customQuestions = eveningQuestionRepository
                .findByUserIdAndMetricTypeAndQuestionDateBetween(userId, MetricType.CUSTOM, startDate, today);

        List<String> logs = new ArrayList<>();
        for (EveningQuestion q : customQuestions) {
            Optional<EveningAnswer> answerOpt = eveningAnswerRepository.findFirstByEveningQuestionIdAndUserId(q.getId(), userId);
            if (answerOpt.isPresent()) {
                EveningAnswer a = answerOpt.get();
                String text = a.getTextValue() != null ? a.getTextValue() : a.getChoiceValue();
                if (text != null && !text.isBlank()) {
                    logs.add(String.format("- %s: %s", q.getQuestionDate(), text));
                }
            }
        }

        if (logs.size() < 3) {
            return List.of(
                    "기록된 건강 일지가 아직 충분하지 않아요.",
                    "매일 저녁 건강 체크를 남겨주시면 진료 시 유용한 패턴을 찾아드릴게요."
            );
        }

        try {
            String historyText = String.join("\n", logs);
            String prompt = String.format("""
                당신은 어르신의 일일 건강 기록을 분석해 병원 진료 시 의사에게 질문할 핵심 증상 변화를 정리해주는 헬스케어 비서입니다.
                다음은 사용자가 최근 한 달 동안 매일 남긴 건강 일지(증상/불편감) 기록입니다.
                
                [일지 기록]
                %s
                
                [작성 지침]
                1. 일지에서 반복되거나 새롭게 나타난 증상 패턴을 분석하세요.
                2. 의사에게 진료 시 문의하거나 확인해야 할 핵심 변화 3~5가지를 한 줄짜리 불릿 포인트로 작성하세요.
                3. 날짜나 시점(예: '8월 초부터', '최근 2주간')을 포함해 구체적으로 서술하세요.
                4. 말투는 정중하고 부드러운 존댓말(~있었어요, ~시작했어요, ~확인되었어요)을 유지하세요.
                5. 다른 인사말이나 부가 설명 없이, 오직 불릿 리스트(각 줄 맨 앞 '- ' 제거하고 문장만) 줄바꿈으로만 출력하세요.
                """, historyText);

            String response = gptClient.ask(prompt);
            return Arrays.stream(response.split("\n"))
                    .map(line -> line.replaceAll("^[\\-\\s*•0-9.]+", "").trim())
                    .filter(line -> !line.isBlank())
                    .limit(5)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            return List.of(
                    "지금은 분석을 불러올 수 없어요. 잠시 후 다시 확인해주세요."
            );
        }
    }

    private String formatRelativeTime(LocalDate pastDate, LocalDate today) {
        long days = ChronoUnit.DAYS.between(pastDate, today);
        if (days < 7) {
            return days + "일 전";
        } else if (days < 30) {
            return (days / 7) + "주 전";
        } else if (days < 365) {
            return (days / 30) + "개월 전";
        } else {
            return (days / 365) + "년 전";
        }
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }
}