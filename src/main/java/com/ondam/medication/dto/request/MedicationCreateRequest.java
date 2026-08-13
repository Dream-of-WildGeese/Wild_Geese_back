package com.ondam.medication.dto.request;

import com.ondam.medication.entity.MedicationDay;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public record MedicationCreateRequest(
        String name,
        List<LocalTime> scheduledTimes,
        Set<MedicationDay> daysOfWeek

) {
}