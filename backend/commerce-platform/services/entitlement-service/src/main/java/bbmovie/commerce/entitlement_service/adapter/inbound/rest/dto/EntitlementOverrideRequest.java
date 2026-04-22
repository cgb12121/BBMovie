package bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record EntitlementOverrideRequest(
        @NotBlank String userId,
        String planId,
        String tier,
        Instant expiresAt,
        String reason
) {
}
