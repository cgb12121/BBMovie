package bbmovie.commerce.payment_orchestrator_service.application.usecase;

import java.util.Map;

import bbmovie.commerce.payment_orchestrator_service.application.port.result.WebhookHandleResult;

public interface WebhookUseCase {
    WebhookHandleResult handle(String provider, String rawBody, Map<String, String> headers);
}

