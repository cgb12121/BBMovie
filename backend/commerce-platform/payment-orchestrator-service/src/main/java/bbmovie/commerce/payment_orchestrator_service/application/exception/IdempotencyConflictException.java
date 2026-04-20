package bbmovie.commerce.payment_orchestrator_service.application.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}

