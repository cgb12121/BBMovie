package bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto;

import java.util.List;

public record UserEntitlementsResponse(
        String userId,
        int total,
        List<EntitlementRecordResponse> entitlements
) {
}
