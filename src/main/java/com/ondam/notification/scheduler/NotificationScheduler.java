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

        List<NotificationSetting> settings =
                notificationSettingRepository.findAll();

        for (NotificationSetting setting : settings) {

            Long userId = setting.getUser().getId();

            /*
             * 아침 알림
             */
            if (setting.isMorningEnabled()
                    && setting.getMorningTime() != null
                    && setting.getMorningTime().equals(now)) {

                notificationService.createNotification(
                        userId,
                        NotificationType.MORNING_QUESTION,
                        "아침 연결 질문",
                        "오늘의 아침 질문이 도착했어요.",
                        LocalDateTime.now(
                                ZoneId.of("Asia/Seoul")
                        )
                );

                webPushService.sendPush(
                        userId,
                        "아침 연결 질문",
                        "오늘의 아침 질문이 도착했어요."
                );
            }

            /*
             * 저녁 알림
             */
            if (setting.isEveningEnabled()
                    && setting.getEveningTime() != null
                    && setting.getEveningTime().equals(now)) {

                notificationService.createNotification(
                        userId,
                        NotificationType.EVENING_CHECK,
                        "저녁 건강 체크",
                        "오늘의 건강 체크 시간이 되었어요.",
                        LocalDateTime.now(
                                ZoneId.of("Asia/Seoul")
                        )
                );

                webPushService.sendPush(
                        userId,
                        "저녁 건강 체크",
                        "오늘의 건강 체크 시간이 되었어요."
                );
            }
        }

        /*
         * =============================
         * 복약 알림
         * =============================
         */

        List<MedicationSchedule> medicationSchedules =
                medicationScheduleRepository.findAll();

        for (MedicationSchedule schedule : medicationSchedules) {

            Long userId =
                    schedule.getMedication()
                            .getUser()
                            .getId();

            // 복약 스케줄 OFF
            if (!schedule.isEnabled()) {
                continue;
            }

            // 복약 시간이 설정되지 않은 경우
            if (schedule.getScheduledTime() == null) {
                continue;
            }

            // 현재 시간이 복약 시간과 정확히 같은지 확인
            if (!schedule.getScheduledTime().equals(now)) {
                continue;
            }

            // 오늘이 복약 요일인지 확인
            if (!schedule.getDaysOfWeek().contains(today)) {
                continue;
            }

            NotificationSetting notificationSetting =
                    notificationSettingRepository
                            .findByUserId(userId)
                            .orElse(null);

            // 복약 알림 설정이 없거나 OFF
            if (notificationSetting == null
                    || !notificationSetting.isMedicationEnabled()) {
                continue;
            }

            notificationService.createNotification(
                    userId,
                    NotificationType.MEDICATION,
                    "복약 알림",
                    schedule.getMedication().getName()
                            + " 복용 시간이에요.",
                    LocalDateTime.now(
                            ZoneId.of("Asia/Seoul")
                    )
            );

            webPushService.sendPush(
                    userId,
                    "복약 알림",
                    schedule.getMedication().getName()
                            + " 복용 시간이에요."
            );
        }
    }
}