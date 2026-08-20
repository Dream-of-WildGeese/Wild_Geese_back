package com.ondam.notification.scheduler;

import com.ondam.medication.entity.MedicationDay;
import com.ondam.medication.entity.MedicationSchedule;
import com.ondam.medication.repository.MedicationScheduleRepository;
import com.ondam.notification.entity.NotificationType;
import com.ondam.notification.service.NotificationService;
import com.ondam.notification.service.WebPushService;
import com.ondam.user.entity.NotificationSetting;
import com.ondam.user.repository.NotificationSettingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final NotificationSettingRepository notificationSettingRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final WebPushService webPushService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void createScheduledNotifications() {

        LocalTime now = LocalTime.now(
                        ZoneId.of("Asia/Seoul")
                )
                .withSecond(0)
                .withNano(0);

        MedicationDay today =
                MedicationDay.valueOf(
                        LocalDate.now(
                                        ZoneId.of("Asia/Seoul")
                                )
                                .getDayOfWeek()
                                .name()
                );

        /*
         * =============================
         * 아침 / 저녁 알림
         * =============================
         */

        List<MedicationSchedule> medicationSchedules =
                medicationScheduleRepository.findAll();

        for (MedicationSchedule schedule : medicationSchedules) {

            Long userId =
                    schedule.getMedication()
                            .getUser()
                            .getId();

            // 1. 복약 스케줄 OFF
            if (!schedule.isEnabled()) {
                continue;
            }

            // 2. 약 자체가 비활성화된 경우
            if (!schedule.getMedication().isActive()) {
                continue;
            }

            // 3. 복약 시간이 없는 경우
            if (schedule.getScheduledTime() == null) {
                continue;
            }

            // 4. 현재 시간이 복약 시간과 정확히 같은지 확인
            if (!schedule.getScheduledTime().equals(now)) {
                continue;
            }

            // 5. 오늘 복용하는 약인지 확인
            if (!schedule.getDaysOfWeek().contains(today)) {
                continue;
            }

            // 6. 사용자 복약 알림 설정 확인
            NotificationSetting notificationSetting =
                    notificationSettingRepository
                            .findByUserId(userId)
                            .orElse(null);

            if (notificationSetting == null
                    || !notificationSetting.isMedicationEnabled()) {
                continue;
            }

            // 7. 알림 내용 생성
            String title = "복약 알림";

            String content =
                    schedule.getMedication().getName()
                            + " "
                            + schedule.getScheduledTime()
                            + " 복용 시간이에요.";

            // 8. 오늘 이미 같은 복약 알림을 보냈으면 건너뜀
            if (notificationService.hasMedicationNotificationToday(
                    userId,
                    title,
                    content
            )) {
                continue;
            }

            // 9. 내부 알림 저장
            notificationService.createNotification(
                    userId,
                    NotificationType.MEDICATION,
                    title,
                    content,
                    LocalDateTime.now(
                            ZoneId.of("Asia/Seoul")
                    )
            );

            // 10. 실제 Web Push
            webPushService.sendPush(
                    userId,
                    title,
                    content
            );
        }


    }
}