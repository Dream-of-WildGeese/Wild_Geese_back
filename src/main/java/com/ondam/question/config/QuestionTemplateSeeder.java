package com.ondam.question.config;

import com.ondam.question.entity.AnswerType;
import com.ondam.question.entity.MetricType;
import com.ondam.question.entity.QuestionTemplate;
import com.ondam.question.repository.QuestionTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** local 프로필에서만 QUESTION_TEMPLATE 최소 시드 데이터를 채워 넣는다. */
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

        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CONDITION)
                .content("오늘 컨디션은 어떠셨나요?")
                .answerType(AnswerType.CHOICE)
                .choices("[{\"label\":\"매우 나쁨\",\"value\":1},{\"label\":\"나쁨\",\"value\":2},{\"label\":\"보통\",\"value\":3},{\"label\":\"좋음\",\"value\":4},{\"label\":\"매우 좋음\",\"value\":5}]")
                .build());

        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.SLEEP)
                .content("어젯밤 잠은 잘 주무셨나요?")
                .answerType(AnswerType.CHOICE)
                .choices("[{\"label\":\"잘 못 잤음\",\"value\":1},{\"label\":\"보통\",\"value\":2},{\"label\":\"잘 잤음\",\"value\":3}]")
                .build());

        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.MEAL)
                .content("오늘 식사는 잘 챙기셨나요?")
                .answerType(AnswerType.CHOICE)
                .choices("[{\"label\":\"잘 못 챙김\",\"value\":1},{\"label\":\"보통\",\"value\":2},{\"label\":\"잘 챙김\",\"value\":3}]")
                .build());

        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.ACTIVITY)
                .content("오늘 활동량은 어느 정도였나요?")
                .answerType(AnswerType.CHOICE)
                .choices("[{\"label\":\"적음\",\"value\":1},{\"label\":\"보통\",\"value\":2},{\"label\":\"많음\",\"value\":3}]")
                .build());

        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.BODY)
                .content("특별히 불편하거나 아픈 곳이 있으셨나요?")
                .answerType(AnswerType.TEXT)
                .choices(null)
                .build());

        questionTemplateRepository.save(QuestionTemplate.builder()
                .metricType(MetricType.CUSTOM)
                .content("오늘 혈압을 측정하셨다면 수치를 알려주세요. (고혈압 관리)")
                .answerType(AnswerType.TEXT)
                .choices(null)
                .build());
    }
}
