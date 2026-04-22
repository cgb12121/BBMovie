package bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto;

import bbmovie.commerce.entitlement_service.domain.EntitlementStatus;

import java.time.Instant;

public record EntitlementRecordResponse(
        String entitlementId,
        String userId,
        String subscriptionId,
        String planId,
        String campaignId,
        String tier,
        EntitlementStatus status,
        Instant startsAt,
        Instant endsAt,
        Instant updatedAt
) {
}
