package com.ondam.family.dto.response;

public record FamilyCreateResponse(
        Long familyId,
        String name,
        String inviteCode
) {
}