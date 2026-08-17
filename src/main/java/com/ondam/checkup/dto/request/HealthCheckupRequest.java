package com.ondam.checkup.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record HealthCheckupRequest(
        @NotNull(message = "검진 날짜는 필수입니다.")
        LocalDate checkupDate,

        @NotBlank(message = "검진 종류를 입력해주세요.")
        String checkupType,

        String hospitalName
) {}