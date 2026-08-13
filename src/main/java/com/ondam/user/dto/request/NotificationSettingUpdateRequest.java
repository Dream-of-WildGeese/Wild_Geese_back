package com.ondam.user.dto.request;

import com.ondam.user.entity.DayOfWeek;

import java.time.LocalTime;

public record NotificationSettingUpdateRequest(LocalTime morningTime,
                                               boolean morningEnabled,
                                               LocalTime eveningTime,
                                               boolean eveningEnabled,
                                               boolean reportEnabled,
                                               DayOfWeek reportDayOfWeek) {
}
