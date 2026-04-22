package bbmovie.commerce.subscription_service.adapter.inbound.rest.dto;

import bbmovie.commerce.subscription_service.domain.SubscriptionStatus;

import java.time.Instant;

public record SubscriptionResponse(
        String subscriptionId,
        String userId,
        String planId,
        String campaignId,
        String sourcePaymentId,
        SubscriptionStatus status,
        Instant startsAt,
        Instant endsAt,
        boolean autoRenew
) {
}
