package com.ondam.family.dto.response;

import com.ondam.user.entity.Gender;
import com.ondam.user.entity.UserRole;

public record FamilyMemberResponse(
        Long userId,
        String email,
        UserRole role,
        Gender gender
) {
}