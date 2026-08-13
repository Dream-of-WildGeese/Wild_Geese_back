package com.ondam.question.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.question.dto.response.MorningQuestionResponse;
import com.ondam.question.service.MorningQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "아침 질문", description = "가족 공통 연결 질문 및 답변 API")
@RestController
@RequestMapping("/api/v1/morning")
@RequiredArgsConstructor
public class MorningController {

    private final MorningQuestionService morningQuestionService;

    @Operation(summary = "오늘의 아침 질문 조회",
            description = "가족 공통 질문 1개와 가족 전원의 답변을 함께 반환합니다.")
    @GetMapping("/today")
    public ApiResponse<MorningQuestionResponse> getTodayQuestion(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(morningQuestionService.getTodayQuestion(userId));
    }
}