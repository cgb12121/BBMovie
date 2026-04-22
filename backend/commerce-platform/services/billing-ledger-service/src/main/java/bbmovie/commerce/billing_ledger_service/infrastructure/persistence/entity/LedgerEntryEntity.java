package bbmovie.commerce.billing_ledger_service.infrastructure.persistence.entity;

import bbmovie.commerce.billing_ledger_service.domain.LedgerEntryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ledger_entries")
public class LedgerEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, length = 64)
    private String paymentId;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 64)
    private LedgerEntryType entryType;

    @Column(name = "provider", length = 32)
    private String provider;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "external_reference_id", length = 128)
    private String externalReferenceId;

    @Column(name = "user_id", length = 128)
    private String userId;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "purpose", length = 255)
    private String purpose;

    @Column(name = "subscription_id", length = 128)
    private String subscriptionId;

    @Column(name = "subscription_campaign_id", length = 128)
    private String subscriptionCampaignId;

    @Lob
    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
