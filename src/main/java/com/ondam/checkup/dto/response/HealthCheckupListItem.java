package com.ondam.checkup.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record HealthCheckupListItem(
        Long checkupId,
        LocalDate checkupDate,
        String checkupType,
        String hospitalName,
        boolean isCompleted,
        Integer reminderDaysBefore
) {
}