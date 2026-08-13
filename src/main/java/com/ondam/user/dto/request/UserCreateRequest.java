package com.ondam.user.dto.request;

import com.ondam.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
        String email,
        String password,
        String name,
        UserRole role
) {
}