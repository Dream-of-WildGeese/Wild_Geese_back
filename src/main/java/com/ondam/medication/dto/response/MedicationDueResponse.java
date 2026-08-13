package com.ondam.medication.dto.response;

import com.ondam.medication.entity.MedicationLogStatus;

import java.time.LocalTime;

public record MedicationDueResponse(
        Long medicationId,
        Long scheduleId,
        String name,
        LocalTime scheduledTime
) {
}