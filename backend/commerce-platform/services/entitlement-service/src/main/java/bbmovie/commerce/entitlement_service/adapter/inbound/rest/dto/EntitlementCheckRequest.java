package bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record EntitlementCheckRequest(
        @NotBlank String userId,
        @NotBlank String resourceId,
        @NotBlank String action,
        String contentPackage
) {
}
