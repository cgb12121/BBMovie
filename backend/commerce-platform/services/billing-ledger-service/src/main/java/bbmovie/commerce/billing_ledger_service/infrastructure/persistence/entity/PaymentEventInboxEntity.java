package bbmovie.commerce.billing_ledger_service.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "payment_event_inbox",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_event_inbox_event_id", columnNames = "event_id")
)
public class PaymentEventInboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payment_id", nullable = false, length = 64)
    private String paymentId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
