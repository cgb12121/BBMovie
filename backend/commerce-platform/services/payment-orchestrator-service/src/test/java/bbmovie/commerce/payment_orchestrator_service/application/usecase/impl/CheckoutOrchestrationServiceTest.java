package bbmovie.commerce.payment_orchestrator_service.application.usecase.impl;

import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.CheckoutRequest;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.PaymentProviderRegistry;
import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment.PaymentCreationPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.persistence.PaymentRepositoryPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.PaymentResult;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.support.PaymentDomainEventPublisher;
import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;
import bbmovie.commerce.payment_orchestrator_service.domain.model.OrchestratorPaymentId;
import bbmovie.commerce.payment_orchestrator_service.domain.model.Payment;
import bbmovie.commerce.payment_orchestrator_service.domain.model.ProviderPaymentId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutOrchestrationServiceTest {

    @Mock
    private PaymentProviderRegistry registry;
    @Mock
    private PaymentCreationPort paymentCreationPort;
    @Mock
    private PaymentRepositoryPort paymentRepository;
    @Mock
    private PaymentDomainEventPublisher domainEventPublisher;

    @InjectMocks
    private CheckoutOrchestrationService service;

    @Test
    void should_create_payment_and_publish_domain_events() {
        CheckoutRequest request = new CheckoutRequest("u1", "u1@test.com", ProviderType.STRIPE, BigDecimal.TEN, "USD", "subscription", Map.of());
        PaymentResult providerResult = new PaymentResult(
                new OrchestratorPaymentId(java.util.UUID.randomUUID()),
                ProviderType.STRIPE,
                new ProviderPaymentId("sp-1"),
                PaymentStatus.PENDING,
                "https://checkout.stripe.com/test",
                "secret_123",
                Map.of()
        );
        when(registry.getRequiredCreationProvider(ProviderType.STRIPE)).thenReturn(paymentCreationPort);
        when(paymentCreationPort.createPayment(any())).thenReturn(providerResult);
        when(paymentRepository.save(any(Payment.class), eq(ProviderType.STRIPE))).thenAnswer(invocation -> invocation.getArgument(0));

        service.checkout("k1", request);

        verify(domainEventPublisher).publish(any(Payment.class), eq(ProviderType.STRIPE));
    }
}

