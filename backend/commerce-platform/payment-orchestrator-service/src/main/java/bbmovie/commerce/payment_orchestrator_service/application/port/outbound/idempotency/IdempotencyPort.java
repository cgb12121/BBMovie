package bbmovie.commerce.payment_orchestrator_service.application.port.outbound.idempotency;

import bbmovie.commerce.payment_orchestrator_service.application.config.IdempotencyOperation;

import java.util.Optional;

public interface IdempotencyPort {
    <T> Optional<T> get(IdempotencyOperation op, String key, Class<T> responseType);

    void put(IdempotencyOperation op, String key, Object response);
}

