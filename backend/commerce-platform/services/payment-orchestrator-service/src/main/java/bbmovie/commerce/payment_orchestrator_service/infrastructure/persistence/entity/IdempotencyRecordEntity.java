package bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity;

import bbmovie.commerce.payment_orchestrator_service.application.config.IdempotencyOperation;
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
        name = "po_idempotency_records",
        indexes = {
                @Index(name = "idx_po_idempotency_key", columnList = "operation,idempotencyKey", unique = true)
        }
)
public class IdempotencyRecordEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IdempotencyOperation operation;

    @Column(nullable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Column(columnDefinition = "TEXT")
    private String responseJson;

    @Column
    private Instant expiresAt;

    @Column(length = 512)
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IdempotencyRecordEntity that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

