package bbmovie.commerce.payment_orchestrator_service.application.port.outbound.dedup;

import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;

public interface WebhookDedupPort {
    boolean recordIfFirst(ProviderType provider, String providerEventId, String rawPayload);
}

