package bbmovie.commerce.payment_orchestrator_service.domain.event;

import bbmovie.commerce.payment_orchestrator_service.domain.model.OrchestratorPaymentId;

import java.time.Instant;

public record PaymentCancelledEvent(
        OrchestratorPaymentId paymentId,
        String reason,
        Instant occurredAt
) implements PaymentDomainEvent {
}

