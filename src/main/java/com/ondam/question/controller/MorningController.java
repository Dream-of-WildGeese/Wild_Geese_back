package com.ondam.question.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.question.dto.response.MorningQuestionResponse;
import com.ondam.question.dto.response.MorningQuestionHistoryItem;
import com.ondam.question.dto.request.MorningAnswerRequest;
import com.ondam.question.service.MorningQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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

    @Operation(summary = "오늘의 아침 질문 답변",
            description = "가족 공통 질문에 대한 내 답변을 저장합니다.")
    @PostMapping("/{questionId}/answers")
    public ApiResponse<Void> submitAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long questionId,
            @RequestBody MorningAnswerRequest request) {
        morningQuestionService.submitAnswer(userId, questionId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "지난 아침 질문 이력 조회",
            description = "지정한 기간(from~to) 동안의 가족 공통 아침 질문 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<MorningQuestionHistoryItem>> getHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return ApiResponse.success(morningQuestionService.getMorningHistory(userId, from, to));
    }
}