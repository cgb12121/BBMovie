package bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto;

import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;
import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;

import java.util.Map;

public record RefundResponse(
        String orchestratorPaymentId,
        ProviderType provider,
        String providerPaymentId,
        PaymentStatus status,
        Map<String, String> providerMetadata
) {
}

