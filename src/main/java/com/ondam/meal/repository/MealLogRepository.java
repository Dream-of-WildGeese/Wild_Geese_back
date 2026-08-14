package com.ondam.meal.repository;

import com.ondam.meal.entity.MealLog;
import com.ondam.meal.entity.MealType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealLogRepository
        extends JpaRepository<MealLog, Long> {

    Optional<MealLog> findByUserIdAndRecordDateAndMealType(
            Long userId,
            LocalDate recordDate,
            MealType mealType
    );

    List<MealLog> findAllByUserIdAndRecordDate(
            Long userId,
            LocalDate recordDate
    );

    long countByUserIdAndRecordDateAndEatenTrue(
            Long userId,
            LocalDate recordDate
    ); // 나중에 meal_count쓸 때 필요
}