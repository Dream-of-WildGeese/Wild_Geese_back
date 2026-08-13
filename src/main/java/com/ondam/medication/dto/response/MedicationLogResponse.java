package com.ondam.medication.dto.response;

import java.time.LocalTime;
import java.util.List;

public record MedicationLogResponse(

        String date,
        int totalCount,
        int takenCount,
        List<MedicationLogItem> medications

) {

    public record MedicationLogItem(
            Long medicationId,
            Long scheduleId,
            String name,
            LocalTime scheduledTime,
            boolean taken
    ) {
    }
}