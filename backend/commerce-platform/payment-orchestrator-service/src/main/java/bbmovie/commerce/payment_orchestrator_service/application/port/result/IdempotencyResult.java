package bbmovie.commerce.payment_orchestrator_service.application.port.result;

public record IdempotencyResult<T>(boolean fromCache, T value) {
}

