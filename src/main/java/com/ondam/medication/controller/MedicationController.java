package com.ondam.medication.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.medication.dto.request.MedicationCreateRequest;
import com.ondam.medication.dto.response.MedicationCreateResponse;
import com.ondam.medication.service.MedicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "복용약", description = "복용양 등록 및 알림 설정 API")
@RestController
@RequestMapping("/api/v1/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MedicationCreateResponse> createMedication(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid MedicationCreateRequest request
    ) {

        return ApiResponse.success(
                medicationService.createMedication(userId, request)
        );
    }
}