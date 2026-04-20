package bbmovie.commerce.payment_orchestrator_service.domain.model;

import java.util.Objects;

public record ProviderPaymentId(String value) {
    public ProviderPaymentId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("provider payment id must not be blank");
        }
    }
}

