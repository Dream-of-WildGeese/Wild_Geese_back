package com.ondam.notification.dto.response;

import com.ondam.notification.entity.NotificationStatus;
import com.ondam.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(

        Long notificationId,
        NotificationType type,
        String title,
        String content,
        LocalDateTime scheduledAt,
        LocalDateTime sentAt,
        NotificationStatus status,
        boolean read

) {
}