package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.model;

import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;
import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.domain.model.ProviderPaymentId;

public record ProviderWebhookEvent(
        ProviderType provider,
        String providerEventId,
        ProviderPaymentId providerPaymentId,
        PaymentStatus normalizedStatus,
        String rawType
) {
}

