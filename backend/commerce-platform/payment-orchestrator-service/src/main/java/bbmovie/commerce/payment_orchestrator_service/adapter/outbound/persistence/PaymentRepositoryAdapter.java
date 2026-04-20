package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.persistence;

import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.persistence.PaymentRepositoryPort;
import bbmovie.commerce.payment_orchestrator_service.domain.model.Money;
import bbmovie.commerce.payment_orchestrator_service.domain.model.OrchestratorPaymentId;
import bbmovie.commerce.payment_orchestrator_service.domain.model.Payment;
import bbmovie.commerce.payment_orchestrator_service.domain.model.ProviderPaymentId;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.PaymentEntity;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.jpa.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment, ProviderType provider) {
        PaymentEntity entity = paymentJpaRepository.findById(payment.id().value())
                .orElseGet(PaymentEntity::new);
        entity.setId(payment.id().value());
        entity.setProvider(provider);
        entity.setAmount(payment.amount().amount());
        entity.setCurrency(payment.amount().currency());
        entity.setMethod(payment.method());
        entity.setStatus(payment.status());
        entity.setProviderPaymentId(payment.providerPaymentId() == null ? null : payment.providerPaymentId().value());
        PaymentEntity saved = paymentJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Payment> findByOrchestratorPaymentId(OrchestratorPaymentId orchestratorPaymentId) {
        return paymentJpaRepository.findById(orchestratorPaymentId.value()).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByProviderPaymentId(ProviderType provider, ProviderPaymentId providerPaymentId) {
        return paymentJpaRepository.findByProviderAndProviderPaymentId(provider, providerPaymentId.value())
                .map(this::toDomain);
    }

    private Payment toDomain(PaymentEntity entity) {
        return Payment.rehydrate(
                new OrchestratorPaymentId(entity.getId()),
                new Money(entity.getAmount(), entity.getCurrency()),
                entity.getMethod(),
                entity.getStatus(),
                entity.getProviderPaymentId() == null ? null : new ProviderPaymentId(entity.getProviderPaymentId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

