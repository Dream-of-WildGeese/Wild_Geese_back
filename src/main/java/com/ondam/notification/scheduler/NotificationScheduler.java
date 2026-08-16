package com.ondam.notification.scheduler;

import com.ondam.medication.entity.MedicationDay;
import com.ondam.medication.entity.MedicationSchedule;
import com.ondam.medication.repository.MedicationScheduleRepository;
import com.ondam.notification.entity.NotificationType;
import com.ondam.notification.service.NotificationService;
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

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void createScheduledNotifications() {

        LocalTime now = LocalTime.now(
                ZoneId.of("Asia/Seoul")
        ).withSecond(0).withNano(0);

        MedicationDay today = MedicationDay.valueOf(
                LocalDate.now(ZoneId.of("Asia/Seoul"))
                        .getDayOfWeek()
                        .name()
        );

        List<NotificationSetting> settings =
                notificationSettingRepository.findAll();

        // 아침 / 저녁 알림
        for (NotificationSetting setting : settings) {

            if (setting.isMorningEnabled()
                    && setting.getMorningTime() != null
                    && setting.getMorningTime().equals(now)) {

                notificationService.createNotification(
                        setting.getUser().getId(),
                        NotificationType.MORNING_QUESTION,
                        "아침 연결 질문",
                        "오늘의 아침 질문이 도착했어요.",
                        LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                );
            }

            if (setting.isEveningEnabled()
                    && setting.getEveningTime() != null
                    && setting.getEveningTime().equals(now)) {

                notificationService.createNotification(
                        setting.getUser().getId(),
                        NotificationType.EVENING_CHECK,
                        "저녁 건강 체크",
                        "오늘의 건강 체크 시간이 되었어요.",
                        LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                );
            }
        }

        // 복약 알림
        List<MedicationSchedule> medicationSchedules =
                medicationScheduleRepository.findAll();

        for (MedicationSchedule schedule : medicationSchedules) {

            if (!schedule.isEnabled()) {
                continue;
            }

            if (!schedule.getScheduledTime().equals(now)) {
                continue;
            }

            if (!schedule.getDaysOfWeek().contains(today)) {
                continue;
            }

            Long userId = schedule.getMedication()
                    .getUser()
                    .getId();

            NotificationSetting notificationSetting =
                    notificationSettingRepository
                            .findByUserId(userId)
                            .orElse(null);

            if (notificationSetting == null
                    || !notificationSetting.isMedicationEnabled()) {
                continue;
            }

            notificationService.createNotification(
                    userId,
                    NotificationType.MEDICATION,
                    "복약 알림",
                    schedule.getMedication().getName() + " 복용 시간이에요.",
                    LocalDateTime.now(ZoneId.of("Asia/Seoul"))
            );
        }
    }
}