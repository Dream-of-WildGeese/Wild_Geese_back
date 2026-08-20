package com.ondam.medication.dto.response;

import lombok.Builder;

import java.time.LocalTime;

@Builder
public record FamilyMedicationStatusResponse(
        Long medicationId,
        String medicationName,
        LocalTime scheduledTime,
        String status
) {
}