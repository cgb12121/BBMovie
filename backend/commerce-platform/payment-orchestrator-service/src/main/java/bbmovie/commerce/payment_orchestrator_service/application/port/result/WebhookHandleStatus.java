package bbmovie.commerce.payment_orchestrator_service.application.port.result;

public enum WebhookHandleStatus {
    ACK,
    DUPLICATE_IGNORED,
    INVALID_SIGNATURE
}

