package com.ondam.family.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FamilyJoinRequest(
        String inviteCode

) {
}