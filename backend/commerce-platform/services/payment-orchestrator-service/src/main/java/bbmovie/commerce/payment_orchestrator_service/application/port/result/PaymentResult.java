package bbmovie.commerce.payment_orchestrator_service.application.port.result;

import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;
import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.domain.model.OrchestratorPaymentId;
import bbmovie.commerce.payment_orchestrator_service.domain.model.ProviderPaymentId;

import java.util.Map;
import java.util.Objects;

public record PaymentResult(
        OrchestratorPaymentId orchestratorPaymentId,
        ProviderType provider,
        ProviderPaymentId providerPaymentId,
        PaymentStatus status,
        String paymentUrl,
        String clientSecret,
        Map<String, String> providerMetadata
) {
    public PaymentResult {
        Objects.requireNonNull(orchestratorPaymentId, "orchestratorPaymentId");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(providerPaymentId, "providerPaymentId");
        Objects.requireNonNull(status, "status");

        providerMetadata = (providerMetadata == null) ? Map.of() : Map.copyOf(providerMetadata);
    }
}

