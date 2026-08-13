package com.ondam.medication.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.medication.dto.request.MedicationCreateRequest;
import com.ondam.medication.dto.request.MedicationUpdateRequest;
import com.ondam.medication.dto.response.MedicationCreateResponse;
import com.ondam.medication.dto.response.MedicationLogResponse;
import com.ondam.medication.dto.response.MedicationResponse;
import com.ondam.medication.service.MedicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "복용약", description = "복용약 등록 및 알림 설정 API")
@RestController
@RequestMapping("/api/v1/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;

    @Operation(
            summary = "복약 정보 등록",
            description = "등록된 복용약의 이름, 복용 시간, 복용 요일을 등록합니다."
    )
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

    @Operation(
            summary = "복약 정보 조회",
            description = "등록된 복용약의 이름, 복용 시간, 복용 요일을 조회합니다."
    )
    @GetMapping
    public ApiResponse<List<MedicationResponse>> getMedications(
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ApiResponse.success(
                medicationService.getMedications(userId)
        );
    }

    @Operation(
            summary = "복약 정보 수정",
            description = "등록된 복용약의 이름, 복용 시간, 복용 요일을 수정합니다."
    )
    @PutMapping("/{medicationId}")
    public ApiResponse<Void> updateMedication(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long medicationId,
            @RequestBody @Valid MedicationUpdateRequest request
    ) {

        medicationService.updateMedication(
                userId,
                medicationId,
                request
        );

        return ApiResponse.success(null);
    }

    @Operation(
            summary = "복약 삭제",
            description = "등록된 복용약을 삭제합니다."
    )
    @DeleteMapping("/{medicationId}")
    public ApiResponse<Void> deleteMedication(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long medicationId
    ) {

        medicationService.deleteMedication(
                userId,
                medicationId
        );

        return ApiResponse.success(null);
    }

    @Operation(
            summary = "날짜별 복약 기록 조회",
            description = "선택한 날짜의 복약 일정과 복약 기록 여부를 조회합니다."
    )
    @GetMapping("/logs")
    public ApiResponse<MedicationLogResponse> getMedicationLogs(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam LocalDate date
    ) {

        return ApiResponse.success(
                medicationService.getMedicationLogs(userId, date)
        );
    }

}