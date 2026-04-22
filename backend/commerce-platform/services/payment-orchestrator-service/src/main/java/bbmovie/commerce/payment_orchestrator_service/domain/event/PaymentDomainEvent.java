package bbmovie.commerce.payment_orchestrator_service.domain.event;

import bbmovie.commerce.payment_orchestrator_service.domain.model.OrchestratorPaymentId;

import java.time.Instant;

public sealed interface PaymentDomainEvent 
    permits PaymentCreatedEvent, PaymentSucceededEvent,
            PaymentFailedEvent, PaymentCancelledEvent,
            PaymentExpiredEvent, PaymentRefundedEvent {
    OrchestratorPaymentId paymentId();

    Instant occurredAt();
}

