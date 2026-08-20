package com.ondam.notification.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.notification.dto.response.NotificationResponse;
import com.ondam.notification.service.NotificationService;
import com.ondam.notification.service.WebPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name ="알림", description= "알림 조회 및 읽음 처리 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final WebPushService webPushService;

    @Operation(
            summary = "알림 목록 조회",
            description = "현재 사용자의 알림 목록을 최신순으로 조회합니다."
    )
    @GetMapping
    public ApiResponse<Page<NotificationResponse>> getNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        return ApiResponse.success(
                notificationService.getNotifications(
                        userId,
                        page,
                        size
                )
        );
    }

    @Operation(
            summary = "알림 읽음 처리",
            description = "선택한 알림을 읽음 상태로 변경합니다."
    )
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> readNotification(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long notificationId
    ) {

        notificationService.readNotification(
                userId,
                notificationId
        );

        return ApiResponse.success(null);
    }

    @Operation(summary = "Web Push 테스트")
    @PostMapping("/test-push")
    public ApiResponse<Void> testPush(
            @RequestHeader("X-User-Id") Long userId
    ) {

        webPushService.sendPush(
                userId,
                "온담 테스트 알림",
                "Web Push 테스트에 성공했어요!"
        );

        return ApiResponse.success(null);
    }
}