package com.ondam.medication.dto.response;

import com.ondam.medication.entity.MedicationDay;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public record MedicationResponse(
        Long medicationId,
        String name,
        List<ScheduleResponse> schedules
) {

    public record ScheduleResponse(
            Long scheduleId,
            LocalTime scheduledTime,
            Set<MedicationDay> daysOfWeek,
            boolean enabled
    ) {
    }
}