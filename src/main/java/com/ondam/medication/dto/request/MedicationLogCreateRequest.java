package com.ondam.medication.dto.request;

import com.ondam.medication.entity.MedicationLogStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MedicationLogCreateRequest(
        Long scheduleId,
        LocalDate recordDate,
        MedicationLogStatus status

) {
}