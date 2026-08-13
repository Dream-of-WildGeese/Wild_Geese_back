package com.ondam.medication.dto.request;

import com.ondam.medication.entity.MedicationLogStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record MedicationLogUpdateRequest(
        LocalDate recordDate,
        List<MedicationLogItem> logs
) {
    public record MedicationLogItem(
            Long scheduleId,
            MedicationLogStatus status
    ) {
    }
}