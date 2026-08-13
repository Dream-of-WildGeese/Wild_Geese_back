package com.ondam.user.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.user.dto.request.HealthProfileUpdateRequest;
import com.ondam.user.dto.request.NotificationSettingUpdateRequest;
import com.ondam.user.dto.request.UserCreateRequest;
import com.ondam.user.dto.response.HealthProfileResponse;
import com.ondam.user.dto.response.NotificationSettingResponse;
import com.ondam.user.dto.response.UserCreateResponse;
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
}