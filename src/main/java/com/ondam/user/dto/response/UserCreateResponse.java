package com.ondam.user.dto.response;

public record UserCreateResponse(
        Long userId,
        String email,
        String password,
        boolean onboardingCompleted
) {
}