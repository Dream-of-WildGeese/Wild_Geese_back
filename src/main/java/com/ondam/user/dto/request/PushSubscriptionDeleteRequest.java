package com.ondam.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PushSubscriptionDeleteRequest(

        @NotBlank
        String endpoint

) {
}