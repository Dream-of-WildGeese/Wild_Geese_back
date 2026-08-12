package com.ondam.question.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.question.dto.response.EveningQuestionResponse;
import com.ondam.question.dto.request.EveningAnswerSubmitRequest;
import com.ondam.question.service.EveningQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "저녁 질문", description = "저녁 건강 체크 질문 및 답변 API")
@RestController
@RequestMapping("/api/v1/evening")
@RequiredArgsConstructor
public class EveningController {

    private final EveningQuestionService eveningQuestionService;

    @Operation(summary = "오늘의 저녁 질문 조회")
    @GetMapping("/today")
    public ApiResponse<EveningQuestionResponse> getTodayQuestions(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(eveningQuestionService.getTodayQuestions(userId));
    }

    @Operation(summary = "저녁 질문 답변 제출", description = "일부만 제출해도 됩니다.")
    @PostMapping("/answers")
    public ApiResponse<Void> submitAnswers(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody EveningAnswerSubmitRequest request) {
        eveningQuestionService.submitAnswers(userId, request);
        return ApiResponse.success(null);
    }
}