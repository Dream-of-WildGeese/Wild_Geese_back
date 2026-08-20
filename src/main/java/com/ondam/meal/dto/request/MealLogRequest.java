package com.ondam.meal.dto.request;

import com.ondam.meal.entity.MealType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MealLogRequest(

        LocalDate recordDate,

        MealType mealType,

        Boolean eaten

) {
}