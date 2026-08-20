package com.ondam.user.dto.response;

import com.ondam.user.entity.UserRole;

public record UserCreateResponse(
        Long userId,
        String email,
        String name,
        UserRole role,
        String inviteCode,
        boolean onboardingCompleted
) {
}