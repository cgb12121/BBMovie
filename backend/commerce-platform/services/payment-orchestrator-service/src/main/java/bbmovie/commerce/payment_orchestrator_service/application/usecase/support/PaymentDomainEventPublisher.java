package bbmovie.commerce.payment_orchestrator_service.application.usecase.support;

import bbmovie.commerce.commerce_contracts.contracts.payment.PaymentEventTypes;
import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.event.EventPublisherPort;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentCreatedEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentCancelledEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentDomainEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentExpiredEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentFailedEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentRefundedEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentSucceededEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentDomainEventPublisher {

    private final EventPublisherPort eventPublisher;

    public int publish(Payment payment, ProviderType provider) {
        int published = 0;
        for (PaymentDomainEvent event : payment.pullDomainEvents()) {
            publishOne(payment, event, provider);
            published++;
        }
        return published;
    }

    private void publishOne(Payment payment, PaymentDomainEvent event, ProviderType provider) {
        switch (event) {
            case PaymentCreatedEvent e -> eventPublisher.publish(
                    PaymentEventTypes.PAYMENT_INITIATED_V1,
                    e.paymentId().value().toString(),
                    enrichPayload(payment, Map.of(
                            "provider", provider.name(),
                            "status", "PENDING",
                            "occurredAt", e.occurredAt().toString()
                    ))
            );
            case PaymentSucceededEvent e -> eventPublisher.publish(
                    PaymentEventTypes.PAYMENT_SUCCEEDED_V1,
                    e.paymentId().value().toString(),
                    enrichPayload(payment, Map.of(
                            "provider", provider.name(),
                            "providerPaymentId", e.providerPaymentId().value(),
                            "status", "SUCCEEDED",
                            "occurredAt", e.occurredAt().toString()
                    ))
            );
            case PaymentFailedEvent e -> eventPublisher.publish(
                    PaymentEventTypes.PAYMENT_FAILED_V1,
                    e.paymentId().value().toString(),
                    enrichPayload(payment, Map.of(
                            "provider", provider.name(),
                            "reason", e.reason() == null ? "" : e.reason(),
                            "status", "FAILED",
                            "occurredAt", e.occurredAt().toString()
                    ))
            );
            case PaymentCancelledEvent e -> eventPublisher.publish(
                    PaymentEventTypes.PAYMENT_STATUS_UPDATED_V1,
                    e.paymentId().value().toString(),
                    enrichPayload(payment, Map.of(
                            "provider", provider.name(),
                            "reason", e.reason() == null ? "" : e.reason(),
                            "status", "CANCELLED",
                            "occurredAt", e.occurredAt().toString()
                    ))
            );
            case PaymentExpiredEvent e -> eventPublisher.publish(
                    PaymentEventTypes.PAYMENT_STATUS_UPDATED_V1,
                    e.paymentId().value().toString(),
                    enrichPayload(payment, Map.of(
                            "provider", provider.name(),
                            "reason", e.reason() == null ? "" : e.reason(),
                            "status", "EXPIRED",
                            "occurredAt", e.occurredAt().toString()
                    ))
            );
            case PaymentRefundedEvent e -> eventPublisher.publish(
                    PaymentEventTypes.PAYMENT_REFUNDED_V1,
                    e.paymentId().value().toString(),
                    enrichPayload(payment, Map.of(
                            "provider", provider.name(),
                            "status", "REFUNDED",
                            "occurredAt", e.occurredAt().toString()
                    ))
            );
        }
    }

    private Map<String, Object> enrichPayload(Payment payment, Map<String, Object> basePayload) {
        Map<String, Object> payload = new HashMap<>(basePayload);
        putIfNotBlank(payload, "userId", payment.userId());
        putIfNotBlank(payload, "userEmail", payment.userEmail());
        putIfNotBlank(payload, "purpose", payment.purpose());
        if (!payment.metadata().isEmpty()) {
            payload.put("metadata", payment.metadata());
            putIfNotBlank(payload, "planId", payment.metadata().get("planId"));
            putIfNotBlank(payload, "subscriptionId", payment.metadata().get("subscriptionId"));
            putIfNotBlank(payload, "subscriptionCampaignId", payment.metadata().get("subscriptionCampaignId"));
        }
        return payload;
    }

    private void putIfNotBlank(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }
}

