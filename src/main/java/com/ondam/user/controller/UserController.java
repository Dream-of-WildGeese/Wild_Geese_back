package com.ondam.user.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.user.dto.request.HealthProfileUpdateRequest;
import com.ondam.user.dto.request.NotificationSettingUpdateRequest;
import com.ondam.user.dto.request.PushSubscriptionCreateRequest;
import com.ondam.user.dto.request.UserCreateRequest;
import com.ondam.user.dto.response.*;
import com.ondam.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "건강 프로필 등록 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserCreateResponse> createUser(
            @RequestBody @Valid UserCreateRequest request
    ) {
        return ApiResponse.success(
                userService.createUser(request)
        );
    }

    @Operation(summary = "건강 프로필 등록/수정")
    @PutMapping("/me/healthprofile")
    public ApiResponse<HealthProfileResponse> updateHealthProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid HealthProfileUpdateRequest request
    ) {
        return ApiResponse.success(
                userService.updateHealthProfile(userId, request)
        );
    }

    @Operation(summary = "알람 설정")
    @PutMapping("/me/notificationsetting")
    public ApiResponse<NotificationSettingResponse> updateNotificationSetting(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody NotificationSettingUpdateRequest request
    ) {
        return ApiResponse.success(
                userService.updateNotificationSettings(userId, request)
        );

    }

    @Operation(summary = "알림 설정 조회")
    @GetMapping("/me/notificationsetting")
    public ApiResponse<NotificationSettingResponse> getNotificationSetting(@RequestHeader("X-User-Id") Long userId)
    {
        return ApiResponse.success(
                userService.getNotificationSetting(userId)
        );
    }

    @Operation(
            summary = "온보딩 완료",
            description = "현재 사용자의 온보딩을 완료 상태로 변경합니다."
    )
    @PatchMapping("/me/onboarding/complete")
    public ApiResponse<Void> completeOnboarding(
            @RequestHeader("X-User-Id") Long userId
    ) {

        userService.completeOnboarding(userId);

        return ApiResponse.success(null);
    }

    @Operation(
            summary = "푸시 알림 구독 등록",
            description = "현재 사용자의 브라우저 푸시 알림 구독 정보를 등록합니다."
    )
    @PostMapping("/me/pushsubscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createPushSubscription(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid PushSubscriptionCreateRequest request
    ) {

        userService.createPushSubscription(
                userId,
                request
        );

        return ApiResponse.success(null);
    }

    @Operation(summary = "초대 코드 조회", description = "사용자의 초대코드를 보여줍니다.")
    @PostMapping("/me/invitecode")
    public ApiResponse<InviteCodeResponse> getInviteCode(
            @RequestHeader("X-User-Id") Long userId
    ) {

        InviteCodeResponse response =
                userService.getInviteCode(userId);

        return ApiResponse.success(response);
    }

    @Operation(
            summary = "현재 사용자 정보 조회",
            description = "현재 사용자의 기본 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ApiResponse<UserNameResponse> getMyInfo(
            @RequestHeader("X-User-Id") Long userId
    ) {

        return ApiResponse.success(
                userService.getMyInfo(userId)
        );
    }

}