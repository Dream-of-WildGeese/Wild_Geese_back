package com.ondam.meal.dto.response;

import com.ondam.meal.entity.MealType;

public record MealLogResponse(
        MealType mealType,
        boolean eaten
) {
}