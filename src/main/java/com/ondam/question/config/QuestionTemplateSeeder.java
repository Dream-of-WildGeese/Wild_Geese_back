package com.ondam.question.config;

import com.ondam.question.entity.AnswerType;
import com.ondam.question.entity.MetricType;
import com.ondam.question.entity.QuestionTemplate;
import com.ondam.question.repository.QuestionTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 저녁 5문항 구성:
 * - CONDITION, SLEEP, MEAL, ACTIVITY → 선택지 3단계(CHOICE)로만 답변
 * - CUSTOM(질환 맞춤) → 음성 전용(TEXT). 나중에 GPT가 실시간 생성하는 게 기본,
 *   이 시드들은 AI 호출 실패 시 폴백(fallback)용으로 유지.
 *
 * CUSTOM 질환 커버리지: 온보딩 선택지 기준 6개 + 공통 폴백 1개
 * (고혈압/당뇨병/고지혈증/심장질환/관절·허리 통증/골다공증/공통(기타·없음))
 */
@Profile("local")
@Component
@RequiredArgsConstructor
public class QuestionTemplateSeeder implements CommandLineRunner {

    private final QuestionTemplateRepository questionTemplateRepository;

    @Override
    public void run(String... args) {
        if (questionTemplateRepository.count() > 0) {
            return;
        }

        // ── CONDITION - 컨디션 (선택지 3단계) ──────────────────
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CONDITION)
                .content("오늘 컨디션은 어땠나요?")
                .answerType(AnswerType.CHOICE)
                .choices("""
                        [{"label":"좋았어요","value":3},{"label":"보통이었어요","value":2},{"label":"좀 힘들었어요","value":1}]
                        """)
                .build());

        // ── SLEEP - 수면 (선택지 3단계) ─────────────────────────
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.SLEEP)
                .content("오늘 수면은 어떠셨나요?")
                .answerType(AnswerType.CHOICE)
                .choices("""
                        [{"label":"푹 잤어요","value":3},{"label":"조금 부족했어요","value":2},{"label":"거의 못 잤어요","value":1}]
                        """)
                .build());

        // ── MEAL - 식사 (선택지 3단계) ──────────────────────────
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.MEAL)
                .content("오늘 식사는 어떠셨나요?")
                .answerType(AnswerType.CHOICE)
                .choices("""
                        [{"label":"잘 챙겼어요","value":3},{"label":"한두 끼 걸렀어요","value":2},{"label":"입맛이 없었어요","value":1}]
                        """)
                .build());

        // ── ACTIVITY - 외출활동 (선택지 3단계) ──────────────────
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.ACTIVITY)
                .content("오늘 외출이나 활동은 어떠셨나요?")
                .answerType(AnswerType.CHOICE)
                .choices("""
                        [{"label":"가볍게 움직였어요","value":3},{"label":"집에서 쉬었어요","value":2},{"label":"거의 못 움직였어요","value":1}]
                        """)
                .build());

        // ── CUSTOM - 질환 맞춤 (음성 전용, TEXT) ────────────────
        // ⚠️ 온보딩 선택지(①~⑥ + 기타/없음) 기준. GPT 연동 전까지는 여기서 매칭해서 사용,
        //    연동 후에는 GPT 호출 실패 시 폴백으로만 사용.

        // ① 고혈압
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CUSTOM)
                .content("오늘 짠 음식을 얼마나 드셨는지 편하게 말씀해주세요.")
                .answerType(AnswerType.TEXT)
                .choices(null)
                .targetDisease("고혈압")
                .build());

        // ② 당뇨병
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CUSTOM)
                .content("오늘 혈당 관리는 어떠셨는지, 단 음식은 안 드셨는지 편하게 말씀해주세요.")
                .answerType(AnswerType.TEXT)
                .choices(null)
                .targetDisease("당뇨병")
                .build());

        // ③ 고지혈증
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CUSTOM)
                .content("오늘 기름진 음식은 얼마나 드셨는지 편하게 말씀해주세요.")
                .answerType(AnswerType.TEXT)
                .choices(null)
                .targetDisease("고지혈증")
                .build());

        // ④ 심장질환
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CUSTOM)
                .content("오늘 가슴이 답답하거나 두근거리진 않으셨는지, 숨이 차진 않으셨는지 편하게 말씀해주세요.")
                .answerType(AnswerType.TEXT)
                .choices(null)
                .targetDisease("심장질환")
                .build());

        // ⑤ 관절·허리 통증
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CUSTOM)
                .content("오늘 무릎이나 허리가 불편하지는 않으셨는지 편하게 말씀해주세요.")
                .answerType(AnswerType.TEXT)
                .choices(null)
                .targetDisease("관절·허리 통증")
                .build());

        // ⑥ 골다공증
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CUSTOM)
                .content("오늘 걷거나 움직이실 때 뼈마디가 시리거나 불안정한 느낌은 없으셨는지 편하게 말씀해주세요.")
                .answerType(AnswerType.TEXT)
                .choices(null)
                .targetDisease("골다공증")
                .build());

        // ⑦⑧ 공통 폴백 - "기타" / "없음" 선택 사용자용 (targetDisease=null)
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CUSTOM)
                .content("오늘 몸은 어떠셨는지, 불편한 곳이 있다면 편하게 말씀해주세요.")
                .answerType(AnswerType.TEXT)
                .choices(null)
                .targetDisease(null)
                .build());
    }
}