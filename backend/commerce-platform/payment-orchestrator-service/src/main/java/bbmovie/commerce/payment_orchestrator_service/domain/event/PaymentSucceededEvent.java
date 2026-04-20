package bbmovie.commerce.payment_orchestrator_service.domain.event;

import bbmovie.commerce.payment_orchestrator_service.domain.model.OrchestratorPaymentId;
import bbmovie.commerce.payment_orchestrator_service.domain.model.ProviderPaymentId;

import java.time.Instant;

public record PaymentSucceededEvent(
        OrchestratorPaymentId paymentId,
        ProviderPaymentId providerPaymentId,
        Instant occurredAt
) implements PaymentDomainEvent {
}

