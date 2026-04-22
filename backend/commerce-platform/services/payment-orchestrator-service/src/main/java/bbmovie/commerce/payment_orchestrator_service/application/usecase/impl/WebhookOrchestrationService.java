package bbmovie.commerce.payment_orchestrator_service.application.usecase.impl;

import bbmovie.commerce.commerce_contracts.contracts.payment.PaymentEventTypes;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.PaymentProviderRegistry;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.model.ProviderWebhookEvent;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.model.WebhookPayload;
import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.dedup.WebhookDedupPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.event.EventPublisherPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.persistence.PaymentRepositoryPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment.WebhookPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.WebhookHandleResult;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.WebhookHandleStatus;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.WebhookUseCase;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.support.PaymentDomainEventPublisher;
import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookOrchestrationService implements WebhookUseCase {

    private final PaymentProviderRegistry registry;
    private final WebhookDedupPort dedupService;
    private final PaymentRepositoryPort paymentRepository;
    private final PaymentDomainEventPublisher domainEventPublisher;
    private final EventPublisherPort eventPublisher;

    public WebhookHandleResult handle(String provider, String rawBody, Map<String, String> headers) {
        ProviderType providerType = ProviderType.valueOf(provider.toUpperCase());
        log.info("Webhook orchestration started: provider={}, headers={}", providerType, headers.size());
        WebhookPort p = registry.getRequiredWebhookProvider(providerType);
        WebhookPayload payload = new WebhookPayload(
                headers,
                rawBody,
                headers.getOrDefault("content-type", null),
                Instant.now()
        );

        if (!p.verifyWebhook(payload)) {
            log.warn("Webhook signature invalid: provider={}", providerType);
            return new WebhookHandleResult(WebhookHandleStatus.INVALID_SIGNATURE, "INVALID_SIGNATURE");
        }

        ProviderWebhookEvent evt = p.parseWebhook(payload);
        log.info(
                "Webhook parsed: provider={}, eventId={}, providerPaymentId={}, normalizedStatus={}, rawType={}",
                providerType,
                evt.providerEventId(),
                evt.providerPaymentId().value(),
                evt.normalizedStatus(),
                evt.rawType()
        );
        boolean first = dedupService.recordIfFirst(providerType, evt.providerEventId(), rawBody);
        if (!first) {
            log.info("Webhook duplicate ignored: provider={}, eventId={}", providerType, evt.providerEventId());
            return new WebhookHandleResult(WebhookHandleStatus.DUPLICATE_IGNORED, "DUPLICATE_IGNORED");
        }

        String aggregateKey = evt.providerPaymentId().value();
        var matchedPayment = paymentRepository.findByProviderPaymentId(providerType, evt.providerPaymentId());
        boolean transitionApplied = false;
        if (matchedPayment.isPresent()) {
            var payment = matchedPayment.get();
            log.info(
                    "Matched payment for webhook: provider={}, paymentId={}, currentStatus={}, incomingStatus={}",
                    providerType,
                    payment.id().value(),
                    payment.status(),
                    evt.normalizedStatus()
            );
            if (evt.normalizedStatus() == PaymentStatus.SUCCEEDED && payment.status() == PaymentStatus.PENDING) {
                payment.markSucceeded(evt.providerPaymentId());
                paymentRepository.save(payment, providerType);
                transitionApplied = true;
                log.info("Payment status updated from webhook: paymentId={}, newStatus=SUCCEEDED", payment.id().value());
            } else if (evt.normalizedStatus() == PaymentStatus.FAILED && payment.status() == PaymentStatus.PENDING) {
                payment.markFailed("Provider webhook status: FAILED");
                paymentRepository.save(payment, providerType);
                transitionApplied = true;
                log.info("Payment status updated from webhook: paymentId={}, newStatus=FAILED", payment.id().value());
            } else if (evt.normalizedStatus() == PaymentStatus.REFUNDED && payment.status() == PaymentStatus.SUCCEEDED) {
                payment.refund();
                paymentRepository.save(payment, providerType);
                transitionApplied = true;
                log.info("Payment status updated from webhook: paymentId={}, newStatus=REFUNDED", payment.id().value());
            } else {
                log.info(
                        "Ignoring non-authoritative webhook status: paymentId={}, currentStatus={}, incomingStatus={}, rawType={}",
                        payment.id().value(),
                        payment.status(),
                        evt.normalizedStatus(),
                        evt.rawType()
                );
            }
        }
        if (matchedPayment.isEmpty()) {
            log.warn(
                    "No matching payment found for webhook: provider={}, providerPaymentId={}",
                    providerType,
                    evt.providerPaymentId().value()
            );
        }
        if (matchedPayment.isPresent()) {
            aggregateKey = matchedPayment.get().id().value().toString();
            int publishedFromDomain = domainEventPublisher.publish(matchedPayment.get(), providerType);
            log.info(
                    "Domain event publish result: provider={}, paymentId={}, publishedCount={}",
                    providerType,
                    aggregateKey,
                    publishedFromDomain
            );
            if (publishedFromDomain > 0) {
                return new WebhookHandleResult(
                        WebhookHandleStatus.ACK,
                        "ACK:" + evt.normalizedStatus()
                );
            }
            if (!transitionApplied) {
                return new WebhookHandleResult(
                        WebhookHandleStatus.ACK,
                        "ACK:IGNORED_NON_TRANSITIONAL_" + evt.normalizedStatus()
                );
            }
        }

        String eventType = switch (evt.normalizedStatus()) {
            case SUCCEEDED -> PaymentEventTypes.PAYMENT_SUCCEEDED_V1;
            case FAILED -> PaymentEventTypes.PAYMENT_FAILED_V1;
            case REFUNDED -> PaymentEventTypes.PAYMENT_REFUNDED_V1;
            default -> PaymentEventTypes.PAYMENT_STATUS_UPDATED_V1;
        };
        log.info(
                "Publishing fallback integration event: provider={}, aggregateKey={}, eventType={}, normalizedStatus={}",
                providerType,
                aggregateKey,
                eventType,
                evt.normalizedStatus()
        );
        eventPublisher.publish(
                eventType,
                aggregateKey,
                Map.of(
                        "provider", evt.provider().name(),
                        "providerEventId", evt.providerEventId(),
                        "status", evt.normalizedStatus().name(),
                        "rawType", evt.rawType()
                )
        );

        return new WebhookHandleResult(
            WebhookHandleStatus.ACK, 
            "ACK:" + evt.normalizedStatus()
        );
    }
}

