package bbmovie.commerce.payment_orchestrator_service.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.Objects;
import java.util.UUID;

public record OrchestratorPaymentId(UUID value) {
    public OrchestratorPaymentId {
        Objects.requireNonNull(value, "value");
    }

    public static OrchestratorPaymentId newId() {
        return new OrchestratorPaymentId(UuidCreator.getTimeOrderedEpoch());
    }
}

