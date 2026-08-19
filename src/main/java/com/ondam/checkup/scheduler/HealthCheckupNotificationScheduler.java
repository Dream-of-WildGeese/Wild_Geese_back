package com.ondam.checkup.scheduler;

import com.ondam.checkup.entity.HealthCheckup;
import com.ondam.checkup.repository.HealthCheckupRepository;
import com.ondam.notification.entity.NotificationType;
import com.ondam.notification.service.NotificationService;
import com.ondam.notification.service.WebPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HealthCheckupNotificationScheduler {

    private final HealthCheckupRepository healthCheckupRepository;
    private final NotificationService notificationService;
    private final WebPushService webPushService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendHealthCheckupNotifications() {

        LocalDate today =
                LocalDate.now(ZoneId.of("Asia/Seoul"));

        System.out.println(
                " 건강검진 알림 Scheduler 실행 today=" + today
        );

        // ==========================
        // 1일 전 알림
        // ==========================

        List<HealthCheckup> oneDayBefore =
                healthCheckupRepository.findCheckupsForReminder(
                        today.plusDays(1),
                        1
                );

        for (HealthCheckup checkup : oneDayBefore) {

            String title = "건강검진 일정 알림";
            String content =
                    "내일 " + checkup.getCheckupType()
                            + " 일정이 있어요.";

            System.out.println(
                    " 건강검진 1일 전 알림 userId="
                            + checkup.getUserId()
            );

            // 온담 내부 알림 저장
            notificationService.createNotification(
                    checkup.getUserId(),
                    NotificationType.HEALTH_CHECKUP,
                    title,
                    content,
                    LocalDateTime.now(
                            ZoneId.of("Asia/Seoul")
                    )
            );

            // 실제 Web Push
            webPushService.sendPush(
                    checkup.getUserId(),
                    title,
                    content
            );
        }

        // ==========================
        // 3일 전 알림
        // ==========================

        List<HealthCheckup> threeDaysBefore =
                healthCheckupRepository.findCheckupsForReminder(
                        today.plusDays(3),
                        3
                );

        for (HealthCheckup checkup : threeDaysBefore) {

            String title = "건강검진 일정 알림";
            String content =
                    "3일 후 " + checkup.getCheckupType()
                            + " 일정이 있어요.";

            System.out.println(
                    " 건강검진 3일 전 알림 userId="
                            + checkup.getUserId()
            );

            // 온담 내부 알림 저장
            notificationService.createNotification(
                    checkup.getUserId(),
                    NotificationType.HEALTH_CHECKUP,
                    title,
                    content,
                    LocalDateTime.now(
                            ZoneId.of("Asia/Seoul")
                    )
            );

            // 실제 Web Push
            webPushService.sendPush(
                    checkup.getUserId(),
                    title,
                    content
            );
        }
    }
}