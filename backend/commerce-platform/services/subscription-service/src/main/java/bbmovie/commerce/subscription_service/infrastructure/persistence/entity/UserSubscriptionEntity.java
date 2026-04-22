package bbmovie.commerce.subscription_service.infrastructure.persistence.entity;

import bbmovie.commerce.subscription_service.domain.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_subscriptions")
public class UserSubscriptionEntity {

    @Id
    @Column(name = "subscription_id", nullable = false, updatable = false, length = 36)
    private String subscriptionId;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "plan_id", nullable = false, length = 128)
    private String planId;

    @Column(name = "campaign_id", length = 128)
    private String campaignId;

    @Column(name = "source_payment_id", nullable = false, length = 128)
    private String sourcePaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SubscriptionStatus status;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (subscriptionId == null || subscriptionId.isBlank()) {
            subscriptionId = UUID.randomUUID().toString();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
