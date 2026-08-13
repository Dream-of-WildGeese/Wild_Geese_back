package com.ondam.report.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.report.dto.response.WeeklyReportResponse;
import com.ondam.report.service.WeeklyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "주간 리포트", description = "이번 주 건강 변화를 요약해서 보여주는 API")
@RestController
@RequestMapping("/api/v1/weekly")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @Operation(summary = "내 최신 주간 리포트 조회",
            description = "이번 주 건강 지표 변화를 지난 주와 비교해서 보여줍니다.")
    @GetMapping("/latest")
    public ApiResponse<WeeklyReportResponse> getMyLatestReport(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(weeklyReportService.getWeeklyReport(userId));
    }

    @Operation(summary = "가족 구성원 최신 주간 리포트 조회",
            description = "가족 구성원의 이번 주 건강 지표 변화를 조회합니다.")
    @GetMapping("/family/{userId}/latest")
    public ApiResponse<WeeklyReportResponse> getFamilyLatestReport(
            @PathVariable Long userId) {
        return ApiResponse.success(weeklyReportService.getWeeklyReport(userId));
    }
}