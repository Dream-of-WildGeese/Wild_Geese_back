package com.ondam.family.dto.response;

public record FamilyJoinResponse(
        Long familyId,
        Long connectedUserId,
        String connectedUserName
) {
}