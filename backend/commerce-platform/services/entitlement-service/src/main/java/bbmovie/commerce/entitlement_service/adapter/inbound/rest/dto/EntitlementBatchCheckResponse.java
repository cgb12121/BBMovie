package bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto;

import java.util.List;

public record EntitlementBatchCheckResponse(
        int total,
        List<EntitlementDecisionResponse> decisions
) {
}
