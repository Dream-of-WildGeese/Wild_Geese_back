package com.ondam.family.controller;

import com.ondam.family.dto.request.FamilyCreateRequest;
import com.ondam.family.dto.response.FamilyCreateResponse;
import com.ondam.family.service.FamilyService;
import com.ondam.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "가족", description = "가족 생성·참여 API")
@RestController
@RequestMapping("/api/v1/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    @Operation(
            summary = "가족 생성",
            description = "새로운 가족을 생성하고 초대코드를 발급합니다."
    )
    @PostMapping
    public ApiResponse<FamilyCreateResponse> createFamily(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid FamilyCreateRequest request
    ) {

        return ApiResponse.success(
                familyService.createFamily(userId, request)
        );
    }
}
