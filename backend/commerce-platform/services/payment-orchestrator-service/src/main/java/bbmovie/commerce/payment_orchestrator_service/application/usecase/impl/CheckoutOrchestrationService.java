package bbmovie.commerce.payment_orchestrator_service.application.usecase.impl;

import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.CheckoutRequest;
import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.CheckoutResponse;
import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.mapper.PaymentApiMapper;
import bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.PaymentProviderRegistry;
import bbmovie.commerce.payment_orchestrator_service.application.command.CreatePaymentCommand;
import bbmovie.commerce.payment_orchestrator_service.application.config.IdempotencyOperation;
import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.persistence.PaymentRepositoryPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.PaymentResult;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.CheckoutUseCase;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.support.PaymentDomainEventPublisher;
import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;
import bbmovie.commerce.payment_orchestrator_service.domain.model.Payment;
import bbmovie.commerce.payment_orchestrator_service.domain.model.PaymentMethod;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.idempotency.Idempotent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckoutOrchestrationService implements CheckoutUseCase {

    private final PaymentProviderRegistry registry;
    private final PaymentRepositoryPort paymentRepository;
    private final PaymentDomainEventPublisher domainEventPublisher;

    @Idempotent(
        operation = IdempotencyOperation.CHECKOUT, 
        responseType = CheckoutResponse.class
    )
    public CheckoutResponse checkout(String idempotencyKey, CheckoutRequest req) {
        CreatePaymentCommand cmd = PaymentApiMapper.toCreatePaymentCommand(req);
        Payment payment = Payment.create(
                cmd.amount(),
                mapMethod(req.provider()),
                cmd.userId(),
                cmd.userEmail(),
                cmd.purpose(),
                cmd.metadata()
        );
        PaymentResult providerResult = registry.getRequiredCreationProvider(req.provider())
                .createPayment(cmd);

        payment.registerProviderPayment(providerResult.providerPaymentId());
        if (providerResult.status() == PaymentStatus.SUCCEEDED) {
            payment.markSucceeded(providerResult.providerPaymentId());
        } else if (providerResult.status() == PaymentStatus.FAILED || providerResult.status() == PaymentStatus.CANCELLED) {
            payment.markFailed("Provider returned status: " + providerResult.status().name());
        }

        Payment saved = paymentRepository.save(payment, req.provider());
        domainEventPublisher.publish(saved, req.provider());
        CheckoutResponse response = PaymentApiMapper.toCheckoutResponse(
                new PaymentResult(
                        saved.id(),
                        providerResult.provider(),
                        providerResult.providerPaymentId(),
                        saved.status(),
                        providerResult.paymentUrl(),
                        providerResult.clientSecret(),
                        providerResult.providerMetadata()
                )
        );
        return response;
    }

    private PaymentMethod mapMethod(ProviderType providerType) {
        return switch (providerType) {
            case STRIPE, PAYPAL -> PaymentMethod.CREDIT_CARD;
            case MOMO, ZALOPAY -> PaymentMethod.E_WALLET;
            case VNPAY -> PaymentMethod.BANK_TRANSFER;
        };
    }
}

