package com.ondam.question.config;

import com.ondam.question.entity.AnswerType;
import com.ondam.question.entity.MetricType;
import com.ondam.question.entity.QuestionTemplate;
import com.ondam.question.repository.QuestionTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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

        // CONDITION - 컨디션
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CONDITION)
                .content("오늘 컨디션은 어땠나요?")
                .answerType(AnswerType.CHOICE)
                .choices("""
                        [{"label":"좋았어요","value":3},{"label":"보통이었어요","value":2},{"label":"좀 힘들었어요","value":1}]
                        """)
                .build());

        // SLEEP - 수면
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.SLEEP)
                .content("오늘 수면은 어떠셨나요?")
                .answerType(AnswerType.CHOICE)
                .choices("""
                        [{"label":"푹 잤어요","value":3},{"label":"조금 부족했어요","value":2},{"label":"거의 못 잤어요","value":1}]
                        """)
                .build());

        // MEAL - 식사
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.MEAL)
                .content("오늘 식사는 어떠셨나요?")
                .answerType(AnswerType.CHOICE)
                .choices("""
                        [{"label":"잘 챙겼어요","value":3},{"label":"한두 끼 걸렀어요","value":2},{"label":"입맛이 없었어요","value":1}]
                        """)
                .build());

        // ACTIVITY - 외출활동
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.ACTIVITY)
                .content("오늘 외출이나 활동은 어떠셨나요?")
                .answerType(AnswerType.CHOICE)
                .choices("""
                        [{"label":"가볍게 움직였어요","value":3},{"label":"집에서 쉬었어요","value":2},{"label":"거의 못 움직였어요","value":1}]
                        """)
                .build());

        // BODY 역할 - 공통 몸상태 질문을 CUSTOM 타입의 "공통 폴백"으로 등록 (targetDisease=null)
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CUSTOM)
                .content("오늘 몸 상태는 어떠셨나요?")
                .answerType(AnswerType.CHOICE)
                .choices("""
                        [{"label":"괜찮았어요","value":3},{"label":"조금 불편했어요","value":2},{"label":"많이 불편했어요","value":1}]
                        """)
                .targetDisease(null)
                .build());

        // CUSTOM - 질환 맞춤 예시 (고혈압 전용)
        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CUSTOM)
                .content("오늘 짠 음식을 드셨나요?")
                .answerType(AnswerType.CHOICE)
                .choices("""
                        [{"label":"많이 먹었어요","value":1},{"label":"보통이었어요","value":2},{"label":"싱겁게 먹었어요","value":3}]
                        """)
                .targetDisease("고혈압")
                .build());
    }
}