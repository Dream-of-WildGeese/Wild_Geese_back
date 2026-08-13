package com.ondam.family.dto.response;

import java.util.List;

public record FamilyInfoResponse(
        Long familyId,
        List<FamilyMemberResponse> members
) {
}