package bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.jpa;

import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.WebhookReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookReceiptRepository extends JpaRepository<WebhookReceiptEntity, UUID> {
    Optional<WebhookReceiptEntity> findByProviderAndProviderEventId(ProviderType provider, String providerEventId);
}

