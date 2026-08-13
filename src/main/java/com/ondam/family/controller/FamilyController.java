package com.ondam.family.controller;

import com.ondam.family.dto.request.FamilyJoinRequest;
import com.ondam.family.dto.response.FamilyInfoResponse;
import com.ondam.family.dto.response.FamilyJoinResponse;
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
            summary = "초대코드로 가족 참여",
            description = "발급받은 초대코드를 입력해 기존 가족에 참여합니다."
    )
    @PostMapping("/join")
    public ApiResponse<FamilyJoinResponse> joinFamily(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid FamilyJoinRequest request
    ) {
        return ApiResponse.success(
                familyService.joinFamily(userId, request)
        );
    }

    @Operation(
            summary = "가족 정보 조회",
            description = "현재 사용자가 속한 가족과 가족 구성원 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ApiResponse<FamilyInfoResponse> getMyFamily(
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ApiResponse.success(
                familyService.getMyFamily(userId)
        );
    }

    @Operation(
            summary = "가족 나가기",
            description = "현재 사용자가 가족에서 나갑니다. 가족 생성자는 다른 구성원이 남아 있으면 나갈 수 없습니다."
    )
    @DeleteMapping("/me")
    public ApiResponse<Void> leaveFamily(
            @RequestHeader("X-User-Id") Long userId
    ) {

        familyService.leaveFamily(userId);

        return ApiResponse.success(null);
    }

    @Operation(
            summary = "가족 구성원 내보내기",
            description = "가족 생성자가 특정 가족 구성원을 가족에서 내보냅니다."
    )
    @DeleteMapping("/me/members/{userId}")
    public ApiResponse<Void> removeMember(
            @RequestHeader("X-User-Id") Long requesterId,
            @PathVariable Long userId
    ) {

        familyService.removeMember(requesterId, userId);

        return ApiResponse.success(null);
    }
}
