package com.ondam.user.dto.response;

import com.ondam.user.entity.Gender;
import com.ondam.user.entity.UserRole;
import com.ondam.user.entity.WellnessInterest;

import java.time.LocalDate;
import java.util.List;

public record HealthProfileResponse(
        Long healthProfileId,
        LocalDate birthDate,
        Gender gender,
        List<String> diseases,
        List<WellnessInterest> wellnessInterests
) {
}