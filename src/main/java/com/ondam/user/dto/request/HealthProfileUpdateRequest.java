package com.ondam.user.dto.request;

import com.ondam.user.entity.Gender;
import com.ondam.user.entity.UserRole;
import com.ondam.user.entity.WellnessInterest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record HealthProfileUpdateRequest(

        @NotBlank
        String name,

        @NotNull
        LocalDate birthDate,

        @NotNull
        UserRole role,

        @NotNull
        Gender gender,

        List<String> diseases,

        List<WellnessInterest> wellnessInterests
) {
}