package bbmovie.commerce.entitlement_service.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "entitlement_event_inbox")
public class EntitlementEventInboxEntity {

    @Id
    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payment_id", nullable = false, length = 128)
    private String paymentId;

    @Lob
    @Column(name = "raw_event_json", nullable = false, columnDefinition = "LONGTEXT")
    private String rawEventJson;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
