package com.ondam.checkup.entity;

import com.ondam.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 "진료 시 물어볼 질문" GPT 생성 결과를 캐싱하는 테이블.
 * GPT 생성 답변을 하루에 한 번만 생성하고 그날 안에는 저장된 값을 재사용한다.
 */
@Entity
@Table(name = "doctor_question_cache")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DoctorQuestionCache extends BaseTimeEntity {

    @Id
    private Long userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionsJson;

    @Builder
    public DoctorQuestionCache(Long userId, String questionsJson) {
        this.userId = userId;
        this.questionsJson = questionsJson;
    }

    public void update(String questionsJson) {
        this.questionsJson = questionsJson;
    }
}