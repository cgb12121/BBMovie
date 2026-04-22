package bbmovie.commerce.payment_orchestrator_service.application.usecase.support;

import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.dedup.WebhookDedupPort;
import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebhookDedupService {

    private final WebhookDedupPort dedupPort;

    public boolean recordIfFirst(ProviderType provider, String providerEventId, String rawPayload) {
        return dedupPort.recordIfFirst(provider, providerEventId, rawPayload);
    }
}

