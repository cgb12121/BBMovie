package bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto;

import java.util.List;

public record EntitlementExplainResponse(
        EntitlementDecisionResponse decision,
        List<String> trace
) {
}
