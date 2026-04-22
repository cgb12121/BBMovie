package bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto;

import java.time.Instant;

public record EntitlementDecisionResponse(
        boolean allowed,
        String reasonCode,
        String planId,
        String tier,
        Instant expiresAt,
        Instant decisionTs
) {
}
