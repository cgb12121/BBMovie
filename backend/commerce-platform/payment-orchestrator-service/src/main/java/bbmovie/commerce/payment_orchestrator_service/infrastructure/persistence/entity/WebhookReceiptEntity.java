package bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity;

import bbmovie.commerce.payment_orchestrator_service.application.config.ProviderType;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "po_webhook_receipts",
        indexes = {
                @Index(name = "idx_po_webhook_event", columnList = "provider,providerEventId", unique = true)
        }
)
public class WebhookReceiptEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProviderType provider;

    @Column(nullable = false, length = 256)
    private String providerEventId;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant receivedAt;

    @Column(nullable = false, length = 64)
    private String payloadHash;

    public static WebhookReceiptEntity create(ProviderType provider, String providerEventId, String payloadHash) {
        WebhookReceiptEntity entity = new WebhookReceiptEntity();
        entity.provider = provider;
        entity.providerEventId = providerEventId;
        entity.payloadHash = payloadHash;
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WebhookReceiptEntity that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

