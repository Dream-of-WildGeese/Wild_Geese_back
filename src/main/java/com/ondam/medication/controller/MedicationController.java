package com.ondam.medication.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.medication.dto.request.MedicationCreateRequest;
import com.ondam.medication.dto.request.MedicationLogCreateRequest;
import com.ondam.medication.dto.request.MedicationLogUpdateRequest;
import com.ondam.medication.dto.request.MedicationUpdateRequest;
import com.ondam.medication.dto.response.*;
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

    @Operation(
            summary = "현재 복용약 조회",
            description = "오늘 현재 시간대에 복용해야 하는 약을 조회합니다."
    )
    @GetMapping("/due")
    public ApiResponse<List<MedicationDueResponse>> getDueMedications(
            @RequestHeader("X-User-Id") Long userId
    ) {

        return ApiResponse.success(
                medicationService.getDueMedications(userId)
        );
    }

    @Operation(
            summary = "복약 기록 생성",
            description = "사용자가 복용한 약을 체크하여 복약 기록을 생성합니다."
    )
    @PostMapping("/logs")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createMedicationLog(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid MedicationLogCreateRequest request
    ) {

        medicationService.createMedicationLog(
                userId,
                request
        );

        return ApiResponse.success(null);
    }

    @Operation(
            summary = "복약 기록 수정",
            description = "선택한 날짜의 복약 기록을 수정합니다."
    )
    @PutMapping("/logs")
    public ApiResponse<Void> updateMedicationLogs(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid MedicationLogUpdateRequest request
    ) {

        medicationService.updateMedicationLogs(
                userId,
                request
        );

        return ApiResponse.success(null);
    }

    @Operation(
            summary = "가족 복약 현황 조회",
            description = "같은 가족 구성원의 특정 날짜 복약 여부를 조회합니다."
    )
    @GetMapping("/family/status")
    public ApiResponse<List<FamilyMedicationStatusResponse>> getFamilyMedicationStatus(
            @RequestHeader("X-User-Id") Long requesterUserId,
            @RequestParam Long targetUserId,
            @RequestParam LocalDate date
    ) {

        return ApiResponse.success(
                medicationService.getFamilyMedicationStatus(
                        requesterUserId,
                        targetUserId,
                        date
                )
        );
    }

}