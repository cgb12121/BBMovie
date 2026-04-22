package bbmovie.commerce.entitlement_service.application.service;

import bbmovie.commerce.commerce_contracts.contracts.payment.PaymentEventTypes;
import bbmovie.commerce.entitlement_service.application.dto.PaymentEventEnvelope;
import bbmovie.commerce.entitlement_service.domain.EntitlementStatus;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.EntitlementEventInboxEntity;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.EntitlementRecordEntity;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.EntitlementEventInboxRepository;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.EntitlementRecordRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementProjectionService {

    private final EntitlementEventInboxRepository inboxRepository;
    private final EntitlementRecordRepository recordRepository;
    private final ObjectMapper objectMapper;
    private final EntitlementDecisionService decisionService;

    @Transactional
    public void ingest(String eventId, PaymentEventEnvelope envelope) {
        ingest(eventId, envelope, false);
    }

    @Transactional
    public void ingest(String eventId, PaymentEventEnvelope envelope, boolean replayMode) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (envelope == null || envelope.eventType() == null || envelope.paymentId() == null) {
            throw new IllegalArgumentException("event payload is invalid");
        }
        if (!replayMode && inboxRepository.existsByEventId(eventId)) {
            log.info("Skipping duplicate entitlement event: eventId={}", eventId);
            return;
        }

        Map<String, Object> payload = safePayload(envelope.payload());
        Instant occurredAt = resolveOccurredAt(payload);

        switch (envelope.eventType()) {
            case PaymentEventTypes.PAYMENT_SUCCEEDED_V1 -> onPaymentSucceeded(envelope, payload, occurredAt);
            case PaymentEventTypes.PAYMENT_REFUNDED_V1 -> onPaymentRevoked(envelope, occurredAt);
            case PaymentEventTypes.PAYMENT_STATUS_UPDATED_V1 -> onPaymentStatusUpdated(envelope, payload, occurredAt);
            default -> log.debug("Ignoring event type for entitlement projection: {}", envelope.eventType());
        }

        if (!replayMode) {
            EntitlementEventInboxEntity inbox = new EntitlementEventInboxEntity();
            inbox.setEventId(eventId);
            inbox.setEventType(envelope.eventType());
            inbox.setPaymentId(envelope.paymentId());
            inbox.setRawEventJson(serializeEnvelope(envelope));
            inbox.setProcessedAt(Instant.now());
            inboxRepository.save(inbox);
        }
    }

    private void onPaymentSucceeded(PaymentEventEnvelope envelope, Map<String, Object> payload, Instant occurredAt) {
        String userId = extractFromPayloadOrMetadata(payload, "userId", "user_id");
        if (userId == null) {
            throw new IllegalArgumentException("Payment succeeded event missing userId");
        }
        String planId = extractFromPayloadOrMetadata(payload, "planId", "plan_id", "subscriptionId", "subscription_id");
        String campaignId = extractFromPayloadOrMetadata(payload, "subscriptionCampaignId", "campaignId", "campaign_id");
        String subscriptionId = extractFromPayloadOrMetadata(payload, "subscriptionId", "subscription_id");

        Instant endAt = occurredAt.plusSeconds(30L * 24 * 3600);

        Optional<EntitlementRecordEntity> existing = recordRepository
                .findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(userId, EntitlementStatus.ACTIVE, occurredAt);

        EntitlementRecordEntity entity = existing.orElseGet(EntitlementRecordEntity::new);
        entity.setUserId(userId);
        entity.setSubscriptionId(subscriptionId);
        entity.setPlanId(planId);
        entity.setCampaignId(campaignId);
        entity.setSourcePaymentId(envelope.paymentId());
        entity.setStatus(EntitlementStatus.ACTIVE);
        entity.setTier(resolveTier(planId));
        entity.setStartsAt(occurredAt);
        entity.setEndsAt(endAt);
        entity.setLastEventAt(occurredAt);
        entity.setUpdatedAt(Instant.now());
        recordRepository.save(entity);
        decisionService.evictUserCache(userId);
    }

    private void onPaymentStatusUpdated(PaymentEventEnvelope envelope, Map<String, Object> payload, Instant occurredAt) {
        String status = toStringOrNull(payload.get("status"));
        if ("CANCELLED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status)) {
            onPaymentRevoked(envelope, occurredAt);
        }
    }

    private void onPaymentRevoked(PaymentEventEnvelope envelope, Instant occurredAt) {
        recordRepository.findFirstBySourcePaymentIdOrderByUpdatedAtDesc(envelope.paymentId())
                .ifPresent(entity -> {
                    if (isOutOfOrder(entity, occurredAt)) {
                        log.info("Skipping stale entitlement revoke event for paymentId={} occurredAt={} lastEventAt={}",
                                envelope.paymentId(), occurredAt, entity.getLastEventAt());
                        return;
                    }
                    entity.setStatus(EntitlementStatus.REVOKED);
                    entity.setEndsAt(occurredAt);
                    entity.setLastEventAt(occurredAt);
                    entity.setUpdatedAt(Instant.now());
                    recordRepository.save(entity);
                    decisionService.evictUserCache(entity.getUserId());
                });
    }

    private boolean isOutOfOrder(EntitlementRecordEntity entity, Instant occurredAt) {
        return entity.getLastEventAt() != null && occurredAt.isBefore(entity.getLastEventAt());
    }

    private Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private Instant resolveOccurredAt(Map<String, Object> payload) {
        String occurredAtRaw = toStringOrNull(payload.get("occurredAt"));
        if (occurredAtRaw == null || occurredAtRaw.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(occurredAtRaw);
        } catch (Exception ex) {
            return Instant.now();
        }
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

    private String toStringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String resolveTier(String planId) {
        if (planId == null) {
            return "FREE";
        }
        String normalized = planId.toLowerCase();
        if (normalized.contains("premium")) {
            return "PREMIUM";
        }
        if (normalized.contains("plus")) {
            return "PLUS";
        }
        return "FREE";
    }

    private String serializeEnvelope(PaymentEventEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize entitlement event", e);
        }
    }
}
