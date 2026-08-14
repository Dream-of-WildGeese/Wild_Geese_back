package com.ondam.meal.controller;

import com.ondam.global.common.ApiResponse;
import com.ondam.meal.dto.request.MealLogRequest;
import com.ondam.meal.dto.response.MealLogResponse;
import com.ondam.meal.service.MealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name="식사",description="식사 체크 및 확인 API")
@RestController
@RequestMapping("/api/v1/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @Operation(
            summary = "식사 기록 저장",
            description = "아침, 점심, 저녁 식사 여부를 저장합니다."
    )
    @PostMapping("/logs")
    public ApiResponse<Void> saveMealLog(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid MealLogRequest request
    ) {

        mealService.saveMealLog(userId, request);

        return ApiResponse.success(null);
    }

    @Operation(
            summary = "날짜별 식사 기록 조회",
            description = "선택한 날짜의 아침, 점심, 저녁 식사 기록을 조회합니다."
    )
    @GetMapping("/logs")
    public ApiResponse<List<MealLogResponse>> getMealLogs(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam LocalDate date
    ) {

        return ApiResponse.success(
                mealService.getMealLogs(userId, date)
        );
    }

}
