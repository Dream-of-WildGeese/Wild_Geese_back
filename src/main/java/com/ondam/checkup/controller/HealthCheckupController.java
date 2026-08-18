package com.ondam.checkup.controller;

import com.ondam.checkup.dto.request.HealthCheckupRequest;
import com.ondam.checkup.dto.response.HealthCheckupResponse;
import com.ondam.checkup.service.HealthCheckupService;
import com.ondam.global.common.ApiResponse;
import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "건강검진", description = "건강검진 일정 관리 및 진료 질문 추천 API")
@RestController
@RequestMapping("/api/v1/checkups")
@RequiredArgsConstructor
public class HealthCheckupController {

    private final HealthCheckupService healthCheckupService;

    @Operation(summary = "건강검진 화면 통합 조회", description = "다가오는 검진(D-day), 지난 검진 목록, AI가 추출한 진료 질문 목록을 조회합니다. (나/가족 구성원 공용)")
    @GetMapping
    public ApiResponse<HealthCheckupResponse> getCheckupScreen(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) Long userId) {

        Long targetUserId = (userId != null) ? userId : headerUserId;

        if (targetUserId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return ApiResponse.success(healthCheckupService.getCheckupScreen(targetUserId));
    }

    @Operation(summary = "건강검진 일정 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Long> createCheckup(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid HealthCheckupRequest request) {
        return ApiResponse.success(healthCheckupService.createCheckup(userId, request));
    }

    @Operation(summary = "건강검진 일정 수정")
    @PatchMapping("/{checkupId}")
    public ApiResponse<Void> updateCheckup(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long checkupId,
            @RequestBody @Valid HealthCheckupRequest request) {
        healthCheckupService.updateCheckup(userId, checkupId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "건강검진 일정 삭제")
    @DeleteMapping("/{checkupId}")
    public ApiResponse<Void> deleteCheckup(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long checkupId) {
        healthCheckupService.deleteCheckup(userId, checkupId);
        return ApiResponse.success(null);
    }
}