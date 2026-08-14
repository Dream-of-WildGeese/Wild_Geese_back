package com.ondam.meal.service;

import com.ondam.global.exception.BusinessException;
import com.ondam.global.exception.ErrorCode;
import com.ondam.meal.dto.request.MealLogRequest;
import com.ondam.meal.dto.response.MealLogResponse;
import com.ondam.meal.entity.MealLog;
import com.ondam.meal.repository.MealLogRepository;
import com.ondam.user.entity.User;
import com.ondam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MealService {

    private final UserRepository userRepository;
    private final MealLogRepository mealLogRepository;

    @Transactional
    public void saveMealLog(
            Long userId,
            MealLogRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        Optional<MealLog> existing =
                mealLogRepository
                        .findByUserIdAndRecordDateAndMealType(
                                userId,
                                request.recordDate(),
                                request.mealType()
                        );

        if (existing.isPresent()) {
            existing.get().update(request.eaten());
            return;
        }

        MealLog mealLog = new MealLog(
                user,
                request.recordDate(),
                request.mealType(),
                request.eaten()
        );

        mealLogRepository.save(mealLog);
    }

    public List<MealLogResponse> getMealLogs(
            Long userId,
            LocalDate date
    ) {

        // 사용자 존재 확인
        userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );

        return mealLogRepository
                .findAllByUserIdAndRecordDate(userId, date)
                .stream()
                .map(log -> new MealLogResponse(
                        log.getMealType(),
                        log.isEaten()
                ))
                .toList();
    }

}
