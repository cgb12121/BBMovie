package bbmovie.commerce.entitlement_service.application.rules;

import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementCheckRequest;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.EntitlementRecordEntity;

import java.time.Instant;

public record DecisionContext(
        EntitlementCheckRequest request,
        EntitlementRecordEntity activeRecord,
        Instant now
) {
}
