package bbmovie.commerce.payment_orchestrator_service.application.usecase.impl;

import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.RefundRequest;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.PaymentProviderRegistry;
import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.persistence.PaymentRepositoryPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment.RefundPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.PaymentResult;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.support.PaymentDomainEventPublisher;
import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;
import bbmovie.commerce.payment_orchestrator_service.domain.model.OrchestratorPaymentId;
import bbmovie.commerce.payment_orchestrator_service.domain.model.Payment;
import bbmovie.commerce.payment_orchestrator_service.domain.model.PaymentMethod;
import bbmovie.commerce.payment_orchestrator_service.domain.model.ProviderPaymentId;
import bbmovie.commerce.payment_orchestrator_service.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundOrchestrationServiceTest {

    @Mock
    private PaymentProviderRegistry registry;
    @Mock
    private RefundPort refundPort;
    @Mock
    private PaymentRepositoryPort paymentRepository;
    @Mock
    private PaymentDomainEventPublisher domainEventPublisher;

    @InjectMocks
    private RefundOrchestrationService service;

    @Test
    void should_refund_and_publish_domain_events() {
        String orchestratorPaymentId = java.util.UUID.randomUUID().toString();
        RefundRequest request = new RefundRequest(orchestratorPaymentId, ProviderType.STRIPE, "sp-1", "duplicate");
        PaymentResult providerResult = new PaymentResult(
                new OrchestratorPaymentId(java.util.UUID.randomUUID()),
                ProviderType.STRIPE,
                new ProviderPaymentId("sp-1"),
                PaymentStatus.REFUNDED,
                null,
                null,
                Map.of()
        );
        when(registry.getRequiredRefundProvider(ProviderType.STRIPE)).thenReturn(refundPort);
        when(refundPort.refund(eq(orchestratorPaymentId), any(ProviderPaymentId.class))).thenReturn(providerResult);
        Payment existingPayment = Payment.create(new Money(java.math.BigDecimal.TEN, "USD"), PaymentMethod.CREDIT_CARD);
        existingPayment.markSucceeded(new ProviderPaymentId("sp-1"));
        when(paymentRepository.findByOrchestratorPaymentId(any(OrchestratorPaymentId.class))).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class), eq(ProviderType.STRIPE))).thenAnswer(invocation -> invocation.getArgument(0));

        service.refund("k1", request);

        verify(domainEventPublisher).publish(any(Payment.class), eq(ProviderType.STRIPE));
    }
}

