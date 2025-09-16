package com.example.common.dtos.nats;

import java.time.LocalDateTime;

public record SubscriptionEvent(
        String eventType,        // SUBSCRIBED | RENEWAL_UPCOMING | CANCELLED
        String userId,
        String userEmail,
        String planName,
        LocalDateTime effectiveAt
) {}


