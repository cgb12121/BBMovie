package bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto;

import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefundRequest(
        @NotBlank String orchestratorPaymentId,
        @NotNull ProviderType provider,
        @NotBlank String providerPaymentId,
        String reason
) {
}

