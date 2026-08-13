package com.ondam.notification.service;

import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
import com.ondam.notification.dto.response.NotificationResponse;
import com.ondam.notification.entity.Notification;
import com.ondam.notification.repository.NotificationRepository;
import com.ondam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public Page<NotificationResponse> getNotifications(
            Long userId,
            int page,
            int size
    ) {

        userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        Pageable pageable =
                PageRequest.of(page, size);

        return notificationRepository
                .findAllByUserIdOrderByCreatedAtDesc(
                        userId,
                        pageable
                )
                .map(notification ->
                        new NotificationResponse(
                                notification.getId(),
                                notification.getType(),
                                notification.getTitle(),
                                notification.getContent(),
                                notification.getScheduledAt(),
                                notification.getSentAt(),
                                notification.getStatus(),
                                notification.isRead()
                        )
                );
    }
    @Transactional
    public void readNotification(
            Long userId,
            Long notificationId
    ) {

        Notification notification =
                notificationRepository
                        .findByIdAndUserId(
                                notificationId,
                                userId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.NOTIFICATION_NOT_FOUND
                                )
                        );

        notification.markAsRead();
    }
}

