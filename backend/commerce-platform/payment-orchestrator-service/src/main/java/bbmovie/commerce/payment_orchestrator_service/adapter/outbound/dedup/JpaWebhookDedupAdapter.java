package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.dedup;

import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.dedup.WebhookDedupPort;
import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.crypto.Sha256Hasher;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.WebhookReceiptEntity;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.jpa.WebhookReceiptRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaWebhookDedupAdapter implements WebhookDedupPort {

    private final WebhookReceiptRepository repo;

    @Override
    @Transactional
    public boolean recordIfFirst(ProviderType provider, String providerEventId, String rawPayload) {
        String payloadHash = Sha256Hasher.sha256Hex(rawPayload == null ? "" : rawPayload);

        WebhookReceiptEntity receipt = new WebhookReceiptEntity();
        receipt.setProvider(provider);
        receipt.setProviderEventId(providerEventId);
        receipt.setPayloadHash(payloadHash);

        try {
            repo.saveAndFlush(receipt);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}

