package com.ondam.user.dto.response;

import com.ondam.user.entity.DayOfWeek;
import java.time.LocalTime;

public record NotificationSettingResponse(
        Long notificationSettingId,
        LocalTime morningTime,
        boolean morningEnabled,
        LocalTime eveningTime,
        boolean eveningEnabled,
        boolean reportEnabled,
        DayOfWeek reportDayOfWeek,
        boolean medicationEnabled
) {
}