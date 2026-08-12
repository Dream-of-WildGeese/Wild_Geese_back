package com.ondam.family.controller;

import com.ondam.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "가족", description = "가족 생성·참여 API")
@RestController
@RequestMapping("/api/v1/families")
public class FamilyController {

    @Operation(summary = "가족 생성", description = "가족을 만들고 초대코드를 발급합니다.")
    @PostMapping
    public ApiResponse<Void> createFamily(
            @RequestHeader("X-User-Id") Long userId) {
        // TODO: Day 2부터 실제 로직 구현 (FamilyCreateRequest DTO 생성 등)
        return ApiResponse.success(null);
    }

    @Operation(summary = "초대코드로 가족 참여", description = "발급받은 초대코드로 가족에 참여합니다.")
    @PostMapping("/join")
    public ApiResponse<Void> joinFamily(
            @RequestHeader("X-User-Id") Long userId) {
        // TODO: Day 2부터 실제 로직 구현
        return ApiResponse.success(null);
    }

    @Operation(summary = "가족 정보 조회", description = "내가 속한 가족의 정보를 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<Void> getMyFamily(
            @RequestHeader("X-User-Id") Long userId) {
        // TODO: Day 2부터 실제 로직 구현
        return ApiResponse.success(null);
    }

    @Operation(summary = "가족 연결 해제", description = "특정 사용자를 가족에서 내보내거나 스스로 나갑니다.")
    @DeleteMapping("/me/members/{targetUserId}")
    public ApiResponse<Void> leaveFamily(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long targetUserId) {
        // TODO: Day 2부터 실제 로직 구현
        return ApiResponse.success(null);
    }
}