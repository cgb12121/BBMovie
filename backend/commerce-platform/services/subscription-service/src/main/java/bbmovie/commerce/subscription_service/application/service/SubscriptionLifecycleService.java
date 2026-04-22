package bbmovie.commerce.subscription_service.application.service;

import bbmovie.commerce.commerce_contracts.contracts.payment.PaymentEventTypes;
import bbmovie.commerce.subscription_service.application.dto.PaymentEventEnvelope;
import bbmovie.commerce.subscription_service.domain.SubscriptionStatus;
import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.CampaignEntity;
import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.PlanEntity;
import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.SubscriptionEventInboxEntity;
import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.UserSubscriptionEntity;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.CampaignRepository;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.PlanRepository;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.SubscriptionEventInboxRepository;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionLifecycleService {

    private final SubscriptionEventInboxRepository inboxRepository;
    private final PlanRepository planRepository;
    private final CampaignRepository campaignRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    @Value("${subscription.fallback-plan.duration-days:30}")
    private int fallbackPlanDurationDays;

    @Transactional
    public void ingest(String eventId, PaymentEventEnvelope envelope) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (envelope == null || envelope.eventType() == null || envelope.paymentId() == null) {
            throw new IllegalArgumentException("event payload is invalid");
        }
        if (inboxRepository.existsByEventId(eventId)) {
            log.info("Skipping duplicate subscription event: eventId={}", eventId);
            return;
        }

        switch (envelope.eventType()) {
            case PaymentEventTypes.PAYMENT_SUCCEEDED_V1 -> handlePaymentSucceeded(envelope);
            case PaymentEventTypes.PAYMENT_REFUNDED_V1 -> handlePaymentCancelledOrRefunded(envelope);
            case PaymentEventTypes.PAYMENT_STATUS_UPDATED_V1 -> handlePaymentStatusUpdated(envelope);
            default -> log.debug("Ignoring unsupported payment event type for subscriptions: {}", envelope.eventType());
        }

        SubscriptionEventInboxEntity inbox = new SubscriptionEventInboxEntity();
        inbox.setEventId(eventId);
        inbox.setEventType(envelope.eventType());
        inbox.setPaymentId(envelope.paymentId());
        inbox.setProcessedAt(Instant.now());
        inboxRepository.save(inbox);
    }

    private void handlePaymentSucceeded(PaymentEventEnvelope envelope) {
        Map<String, Object> payload = safePayload(envelope.payload());
        String userId = extractFromPayloadOrMetadata(payload, "userId", "user_id");
        String planId = extractFromPayloadOrMetadata(payload, "planId", "plan_id", "subscriptionId", "subscription_id");
        String campaignId = extractFromPayloadOrMetadata(payload, "subscriptionCampaignId", "campaignId", "campaign_id");

        if (userId == null || planId == null) {
            throw new IllegalArgumentException("PaymentSucceeded event is missing required userId/planId");
        }

        PlanEntity plan = planRepository.findByPlanId(planId)
                .filter(PlanEntity::isActive)
                .orElseGet(() -> createFallbackPlan(planId));

        CampaignEntity campaign = null;
        if (campaignId != null) {
            campaign = campaignRepository.findByCampaignId(campaignId)
                    .filter(CampaignEntity::isActive)
                    .orElse(null);
            if (campaign == null) {
                log.warn("Ignoring unknown or inactive campaignId={} for planId={}", campaignId, plan.getPlanId());
            } else if (!plan.getPlanId().equals(campaign.getPlanId())) {
                log.warn(
                        "Ignoring campaignId={} because campaign.planId={} does not match planId={}",
                        campaignId,
                        campaign.getPlanId(),
                        plan.getPlanId()
                );
                campaign = null;
            }
        }

        Instant now = Instant.now();
        Optional<UserSubscriptionEntity> existing = userSubscriptionRepository
                .findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(userId, SubscriptionStatus.ACTIVE, now);

        UserSubscriptionEntity entity = existing
                .filter(e -> e.getPlanId().equals(plan.getPlanId()))
                .orElseGet(UserSubscriptionEntity::new);

        Instant baseStart = (entity.getSubscriptionId() == null) ? now : max(now, entity.getEndsAt());
        entity.setUserId(userId);
        entity.setPlanId(plan.getPlanId());
        entity.setCampaignId(campaign == null ? null : campaign.getCampaignId());
        entity.setSourcePaymentId(envelope.paymentId());
        entity.setStatus(SubscriptionStatus.ACTIVE);
        entity.setStartsAt(baseStart);
        entity.setEndsAt(baseStart.plusSeconds((long) plan.getDurationDays() * 24 * 3600));
        entity.setAutoRenew(true);
        userSubscriptionRepository.save(entity);

        log.info(
                "Subscription upserted: userId={}, subscriptionId={}, planId={}, campaignId={}, paymentId={}",
                entity.getUserId(),
                entity.getSubscriptionId(),
                entity.getPlanId(),
                entity.getCampaignId(),
                envelope.paymentId()
        );
    }

    private void handlePaymentStatusUpdated(PaymentEventEnvelope envelope) {
        Map<String, Object> payload = safePayload(envelope.payload());
        String status = toStringOrNull(payload.get("status"));
        if ("CANCELLED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status)) {
            handlePaymentCancelledOrRefunded(envelope);
        }
    }

    private void handlePaymentCancelledOrRefunded(PaymentEventEnvelope envelope) {
        userSubscriptionRepository.findFirstBySourcePaymentIdOrderByCreatedAtDesc(envelope.paymentId())
                .ifPresent(entity -> {
                    entity.setStatus(SubscriptionStatus.CANCELLED);
                    entity.setAutoRenew(false);
                    entity.setEndsAt(Instant.now());
                    userSubscriptionRepository.save(entity);
                    log.info(
                            "Subscription cancelled from payment event: subscriptionId={}, paymentId={}",
                            entity.getSubscriptionId(),
                            envelope.paymentId()
                    );
                });
    }

    private Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private String toStringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String extractFromPayloadOrMetadata(Map<String, Object> payload, String... keys) {
        String topLevel = extractFirstNonBlank(payload, keys);
        if (topLevel != null) {
            return topLevel;
        }
        Object metadataObj = payload.get("metadata");
        if (metadataObj instanceof Map<?, ?> metadataMap) {
            for (String key : keys) {
                Object value = metadataMap.get(key);
                String text = toStringOrNull(value);
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private String extractFirstNonBlank(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            String value = toStringOrNull(payload.get(key));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Instant max(Instant left, Instant right) {
        return left.isAfter(right) ? left : right;
    }

    private PlanEntity createFallbackPlan(String planId) {
        PlanEntity fallback = new PlanEntity();
        fallback.setPlanId(planId);
        fallback.setName("External-" + planId);
        fallback.setDurationDays(fallbackPlanDurationDays);
        fallback.setActive(true);
        PlanEntity saved = planRepository.save(fallback);
        log.warn(
                "Created fallback plan for unknown planId={}, durationDays={}",
                planId,
                fallbackPlanDurationDays
        );
        return saved;
    }
}
