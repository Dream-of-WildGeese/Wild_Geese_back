package com.ondam.checkup.dto.response;

import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

@Builder
public record HealthCheckupResponse(
        UpcomingCheckupItem upcomingCheckup,
        List<PastCheckupItem> pastCheckups,
        List<String> doctorQuestions
) {
    @Builder
    public record UpcomingCheckupItem(
            Long checkupId,
            LocalDate checkupDate,
            String checkupType,
            String hospitalName,
            long dDay
    ) {}

    @Builder
    public record PastCheckupItem(
            Long checkupId,
            LocalDate checkupDate,
            String checkupType,
            String hospitalName,
            String relativeTime
    ) {}
}