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

    // 설정 시간부터 2분 이내까지 허용
    private boolean isWithinTimeRange(
            LocalTime scheduledTime,
            LocalTime now
    ) {

        if (scheduledTime == null) {
            return false;
        }

        return !scheduledTime.isAfter(now)
                && scheduledTime.isAfter(now.minusMinutes(2));
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void createScheduledNotifications() {

        LocalTime now = LocalTime.now(
                        ZoneId.of("Asia/Seoul")
                )
                .withSecond(0)
                .withNano(0);

        System.out.println(
                " NotificationScheduler 실행 now = " + now
        );

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

            System.out.println(
                    "[알림 설정 체크]"
                            + " userId=" + userId
                            + ", morningEnabled="
                            + setting.isMorningEnabled()
                            + ", morningTime="
                            + setting.getMorningTime()
                            + ", eveningEnabled="
                            + setting.isEveningEnabled()
                            + ", eveningTime="
                            + setting.getEveningTime()
                            + ", now=" + now
            );

            /*
             * 아침 알림
             */
            if (setting.isMorningEnabled()
                    && isWithinTimeRange(
                    setting.getMorningTime(),
                    now
            )) {

                System.out.println(
                        " 아침 알림 조건 통과 userId="
                                + userId
                );

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
                    && isWithinTimeRange(
                    setting.getEveningTime(),
                    now
            )) {

                System.out.println(
                        " 저녁 알림 조건 통과 userId="
                                + userId
                );

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

        for (MedicationSchedule schedule
                : medicationSchedules) {

            Long userId =
                    schedule.getMedication()
                            .getUser()
                            .getId();

            System.out.println(
                    "[복약 알림 체크]"
                            + " userId=" + userId
                            + ", enabled="
                            + schedule.isEnabled()
                            + ", scheduledTime="
                            + schedule.getScheduledTime()
                            + ", now=" + now
                            + ", today=" + today
                            + ", daysOfWeek="
                            + schedule.getDaysOfWeek()
            );

            // 복약 스케줄 OFF
            if (!schedule.isEnabled()) {
                continue;
            }

            // 기존 equals(now) 대신
            // 2분 범위 허용
            if (!isWithinTimeRange(
                    schedule.getScheduledTime(),
                    now
            )) {
                continue;
            }

            // 오늘이 복약 요일인지 확인
            if (!schedule.getDaysOfWeek()
                    .contains(today)) {
                continue;
            }

            NotificationSetting notificationSetting =
                    notificationSettingRepository
                            .findByUserId(userId)
                            .orElse(null);

            // 복약 알림 설정이 없거나 OFF
            if (notificationSetting == null
                    || !notificationSetting
                    .isMedicationEnabled()) {

                continue;
            }

            System.out.println(
                    " 복약 알림 조건 통과 userId="
                            + userId
            );

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