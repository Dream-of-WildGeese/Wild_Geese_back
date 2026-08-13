package com.ondam.dailylog.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.dailylog.dto.response.DailyLogResponse;
import com.ondam.dailylog.service.DailyLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "일지", description = "매일의 아침/저녁 기록을 모아 보여주는 API")
@RestController
@RequestMapping("/api/v1/daily")
@RequiredArgsConstructor
public class DailyLogController {

    private final DailyLogService dailyLogService;

    @Operation(summary = "내 일지 조회", description = "지정한 날짜(date)의 아침/저녁 답변 요약을 조회합니다.")
    @GetMapping
    public ApiResponse<DailyLogResponse> getDailyLog(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam LocalDate date) {
        return ApiResponse.success(dailyLogService.getDailyLog(userId, date));
    }

    @Operation(summary = "가족 구성원 일지 조회", description = "가족 구성원의 일지를 조회합니다.")
    @GetMapping("/family/{userId}")
    public ApiResponse<DailyLogResponse> getFamilyDailyLog(
            @PathVariable Long userId,
            @RequestParam LocalDate date) {
        return ApiResponse.success(dailyLogService.getDailyLog(userId, date));
    }
}