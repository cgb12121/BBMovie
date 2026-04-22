package bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EntitlementBatchCheckRequest(
        @NotEmpty List<@Valid EntitlementCheckRequest> items
) {
}
