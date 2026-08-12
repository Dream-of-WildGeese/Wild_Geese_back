package com.ondam.user.controller;

import com.ondam.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "사용자 생성 및 온보딩 API")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Operation(
            summary = "사용자 생성 (온보딩 시작)",
            description = "이름, 역할, 출생연도를 입력받아 사용자를 생성하고 userId를 반환합니다."
    )
    @PostMapping
    public ApiResponse<Void> createUser() {
        // 주의: 사용자 생성 시점에는 아직 userId가 없으므로 헤더를 받지 않습니다.
        // TODO: Day 2부터 DTO(예: UserCreateRequest) 생성 및 로직 구현
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "건강 프로필 등록/수정",
            description = "사용자의 질환 및 관심사 목록을 설정합니다."
    )
    @PutMapping("/me/health-profile")
    public ApiResponse<Void> updateHealthProfile(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "알림 설정 등록/수정",
            description = "아침/저녁 질문 및 리포트 알림 수신 여부와 시각을 설정합니다."
    )
    @PutMapping("/me/notification-setting")
    public ApiResponse<Void> updateNotificationSetting(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "웹 푸시 구독 등록",
            description = "브라우저 푸시 알림을 위한 토큰을 저장합니다. (권한 허용 직후 반드시 호출)"
    )
    @PostMapping("/me/push-subscription")
    public ApiResponse<Void> subscribePush(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "온보딩 완료 처리",
            description = "초기 온보딩 과정을 완료 상태로 변경합니다."
    )
    @PostMapping("/me/onboarding/complete")
    public ApiResponse<Void> completeOnboarding(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(null);
    }
}