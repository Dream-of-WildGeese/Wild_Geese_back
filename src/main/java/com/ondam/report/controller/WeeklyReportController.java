package com.ondam.report.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.report.dto.response.WeeklyReportResponse;
import com.ondam.report.service.WeeklyReportService;
import com.ondam.report.dto.response.WeeklyReportHistoryItem;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "주간 리포트", description = "이번 주 건강 변화를 요약해서 보여주는 API")
@RestController
@RequestMapping("/api/v1/weekly")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @Operation(summary = "특정 주 리포트 조회", description = "지정한 주의 건강 지표 변화를 조회합니다.")
    @GetMapping
    public ApiResponse<WeeklyReportResponse> getWeeklyReport(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam LocalDate weekStartDate) {
        return ApiResponse.success(weeklyReportService.getWeeklyReport(userId, weekStartDate));
    }

    @Operation(summary = "내 최신 주간 리포트 조회",
            description = "이번 주 건강 지표 변화를 지난 주와 비교해서 보여줍니다.")
    @GetMapping("/latest")
    public ApiResponse<WeeklyReportResponse> getMyLatestReport(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(weeklyReportService.getWeeklyReport(userId, null));
    }

    @Operation(summary = "가족 구성원 최신 주간 리포트 조회",
            description = "가족 구성원의 이번 주 건강 지표 변화를 조회합니다.")
    @GetMapping("/family/{userId}/latest")
    public ApiResponse<WeeklyReportResponse> getFamilyLatestReport(
            @PathVariable Long userId) {
        return ApiResponse.success(weeklyReportService.getWeeklyReport(userId, null));
    }

    @Operation(summary = "주간 리포트 이력 조회", description = "이 사용자의 모든 주차별 리포트 한줄평을 최신순으로 조회합니다.")
    @GetMapping("/history")
    public ApiResponse<List<WeeklyReportHistoryItem>> getWeeklyReportHistory(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(weeklyReportService.getWeeklyReportHistory(userId));
    }

    @Operation(summary = "가족 구성원 특정 주 리포트 조회", description = "가족 구성원의 지정한 주 건강 지표 변화를 조회합니다.")
    @GetMapping("/family/{userId}")
    public ApiResponse<WeeklyReportResponse> getFamilyWeeklyReport(
            @PathVariable Long userId,
            @RequestParam LocalDate weekStartDate) {
        return ApiResponse.success(weeklyReportService.getWeeklyReport(userId, weekStartDate));
    }

    @Operation(summary = "가족 구성원 주간 리포트 이력 조회", description = "가족 구성원의 모든 주차별 리포트 한줄평을 최신순으로 조회합니다.")
    @GetMapping("/family/{userId}/history")
    public ApiResponse<List<WeeklyReportHistoryItem>> getFamilyWeeklyReportHistory(
            @PathVariable Long userId) {
        return ApiResponse.success(weeklyReportService.getWeeklyReportHistory(userId));
    }
}