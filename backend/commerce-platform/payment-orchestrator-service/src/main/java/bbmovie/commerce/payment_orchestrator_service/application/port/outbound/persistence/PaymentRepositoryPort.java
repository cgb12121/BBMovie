package bbmovie.commerce.payment_orchestrator_service.application.port.outbound.persistence;

import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.domain.model.OrchestratorPaymentId;
import bbmovie.commerce.payment_orchestrator_service.domain.model.Payment;
import bbmovie.commerce.payment_orchestrator_service.domain.model.ProviderPaymentId;

import java.util.Optional;

public interface PaymentRepositoryPort {
    Payment save(Payment payment, ProviderType provider);

    Optional<Payment> findByOrchestratorPaymentId(OrchestratorPaymentId orchestratorPaymentId);

    Optional<Payment> findByProviderPaymentId(ProviderType provider, ProviderPaymentId providerPaymentId);
}

