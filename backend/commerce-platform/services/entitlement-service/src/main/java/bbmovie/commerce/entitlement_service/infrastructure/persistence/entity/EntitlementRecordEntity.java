package bbmovie.commerce.entitlement_service.infrastructure.persistence.entity;

import bbmovie.commerce.entitlement_service.domain.EntitlementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "entitlement_records")
public class EntitlementRecordEntity {

    @Id
    @Column(name = "entitlement_id", nullable = false, updatable = false, length = 36)
    private String entitlementId = UUID.randomUUID().toString();

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "subscription_id", length = 128)
    private String subscriptionId;

    @Column(name = "plan_id", length = 128)
    private String planId;

    @Column(name = "campaign_id", length = 128)
    private String campaignId;

    @Column(name = "tier", length = 64)
    private String tier;

    @Column(name = "source_payment_id", nullable = false, length = 128)
    private String sourcePaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EntitlementStatus status;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_event_at")
    private Instant lastEventAt;
}
