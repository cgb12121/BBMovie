package bbmovie.commerce.payment_orchestrator_service.application.usecase.impl;

import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.RefundRequest;
import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.RefundResponse;
import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.mapper.PaymentApiMapper;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.PaymentProviderRegistry;
import bbmovie.commerce.payment_orchestrator_service.application.config.IdempotencyOperation;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.persistence.PaymentRepositoryPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.PaymentResult;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.RefundUseCase;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.support.PaymentDomainEventPublisher;
import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;
import bbmovie.commerce.payment_orchestrator_service.domain.model.OrchestratorPaymentId;
import bbmovie.commerce.payment_orchestrator_service.domain.model.ProviderPaymentId;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.idempotency.Idempotent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundOrchestrationService implements RefundUseCase {

    private final PaymentProviderRegistry registry;
    private final PaymentRepositoryPort paymentRepository;
    private final PaymentDomainEventPublisher domainEventPublisher;

    @Idempotent(
        operation = IdempotencyOperation.REFUND,
        responseType = RefundResponse.class
    )
    public RefundResponse refund(String idempotencyKey, RefundRequest req) {
        var payment = paymentRepository.findByOrchestratorPaymentId(new OrchestratorPaymentId(java.util.UUID.fromString(req.orchestratorPaymentId())))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + req.orchestratorPaymentId()));
        ProviderPaymentId providerPaymentId = payment.providerPaymentId() != null
                ? payment.providerPaymentId()
                : new ProviderPaymentId(req.providerPaymentId());
        PaymentResult providerResult = registry.getRequiredRefundProvider(req.provider())
                .refund(
                        req.orchestratorPaymentId(),
                        providerPaymentId
                );
        if (providerResult.status() == PaymentStatus.REFUNDED) {
            payment.refund();
            paymentRepository.save(payment, req.provider());
        }
        domainEventPublisher.publish(payment, req.provider());

        RefundResponse response = PaymentApiMapper.toRefundResponse(
                new PaymentResult(
                        payment.id(),
                        providerResult.provider(),
                        providerResult.providerPaymentId(),
                        providerResult.status(),
                        providerResult.paymentUrl(),
                        providerResult.clientSecret(),
                        providerResult.providerMetadata()
                )
        );
        return response;
    }
}

