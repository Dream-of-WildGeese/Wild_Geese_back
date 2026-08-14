package com.ondam.user.dto.request;

public record PushSubscriptionCreateRequest(
        String endpoint,
        String p256dh,
        String auth
) {
}