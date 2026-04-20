package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.idempotency;

import bbmovie.commerce.payment_orchestrator_service.application.IdempotencyCacheService;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.idempotency.IdempotencyPort;
import bbmovie.commerce.payment_orchestrator_service.application.config.IdempotencyOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private final IdempotencyCacheService cacheService;

    @Override
    public <T> Optional<T> get(IdempotencyOperation op, String key, Class<T> responseType) {
        return cacheService.get(op, key, responseType);
    }

    @Override
    public void put(IdempotencyOperation op, String key, Object response) {
        cacheService.put(op, key, response);
    }
}

