package bbmovie.commerce.billing_ledger_service.application.service;

import bbmovie.commerce.billing_ledger_service.application.dto.PaymentEventEnvelope;
import bbmovie.commerce.billing_ledger_service.domain.LedgerEntryType;
import bbmovie.commerce.billing_ledger_service.infrastructure.persistence.entity.LedgerEntryEntity;
import bbmovie.commerce.billing_ledger_service.infrastructure.persistence.entity.PaymentEventInboxEntity;
import bbmovie.commerce.billing_ledger_service.infrastructure.persistence.repo.LedgerEntryRepository;
import bbmovie.commerce.billing_ledger_service.infrastructure.persistence.repo.PaymentEventInboxRepository;
import bbmovie.commerce.commerce_contracts.contracts.payment.PaymentEventTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentLedgerIngestionService {

    private final PaymentEventInboxRepository inboxRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void ingest(String eventId, PaymentEventEnvelope envelope) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (envelope == null || envelope.eventType() == null || envelope.paymentId() == null) {
            throw new IllegalArgumentException("event payload is invalid");
        }
        if (inboxRepository.existsByEventId(eventId)) {
            log.info("Skipping duplicated payment event: eventId={}", eventId);
            return;
        }

        Map<String, Object> payload = envelope.payload() == null ? Map.of() : envelope.payload();
        LedgerEntryEntity entry = new LedgerEntryEntity();
        entry.setEventId(eventId);
        entry.setPaymentId(envelope.paymentId());
        entry.setEntryType(toEntryType(envelope.eventType()));
        entry.setProvider(toStringOrNull(payload.get("provider")));
        entry.setStatus(toStringOrNull(payload.get("status")));
        entry.setAmount(resolveAmount(payload));
        entry.setCurrency(extractFromPayloadOrMetadata(payload, "currency"));
        entry.setExternalReferenceId(extractFirstNonBlank(
                payload,
                "externalReferenceId",
                "external_reference_id",
                "providerPaymentId",
                "providerReferenceId",
                "provider_reference_id"
        ));
        entry.setUserId(extractFromPayloadOrMetadata(payload, "userId", "user_id"));
        entry.setUserEmail(extractFromPayloadOrMetadata(payload, "userEmail", "user_email"));
        entry.setPurpose(extractFromPayloadOrMetadata(payload, "purpose"));
        entry.setSubscriptionId(extractFirstNonBlank(
                payload,
                "subscriptionId",
                "subscription_id",
                "planId",
                "plan_id",
                "planSubscriptionId",
                "entitlementSubscriptionId"
        ));
        entry.setSubscriptionCampaignId(extractFirstNonBlank(
                payload,
                "subscriptionCampaignId",
                "subscription_campaign_id",
                "campaignId",
                "campaign_id"
        ));
        entry.setOccurredAt(resolveOccurredAt(payload));
        entry.setPayloadJson(writePayload(payload));
        ledgerEntryRepository.save(entry);

        PaymentEventInboxEntity inboxEntity = new PaymentEventInboxEntity();
        inboxEntity.setEventId(eventId);
        inboxEntity.setEventType(envelope.eventType());
        inboxEntity.setPaymentId(envelope.paymentId());
        inboxEntity.setProcessedAt(Instant.now());
        inboxRepository.save(inboxEntity);
    }

    private LedgerEntryType toEntryType(String eventType) {
        return switch (eventType) {
            case PaymentEventTypes.PAYMENT_INITIATED_V1 -> LedgerEntryType.PAYMENT_INITIATED;
            case PaymentEventTypes.PAYMENT_SUCCEEDED_V1 -> LedgerEntryType.PAYMENT_SUCCEEDED;
            case PaymentEventTypes.PAYMENT_FAILED_V1 -> LedgerEntryType.PAYMENT_FAILED;
            case PaymentEventTypes.PAYMENT_REFUNDED_V1 -> LedgerEntryType.PAYMENT_REFUNDED;
            default -> LedgerEntryType.PAYMENT_STATUS_UPDATED;
        };
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

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("Failed to serialize payment payload", ex);
            return "{}";
        }
    }

    private String toStringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private BigDecimal resolveAmount(Map<String, Object> payload) {
        String amountRaw = extractFromPayloadOrMetadata(payload, "amount");
        if (amountRaw == null || amountRaw.isBlank()) {
            return null;
        }
        try {
            String normalized = normalizeAmount(amountRaw);
            if (normalized == null || normalized.isBlank()) {
                return null;
            }
            return new BigDecimal(normalized);
        } catch (Exception ex) {
            log.warn("Invalid amount in payment payload: {}", amountRaw);
            return null;
        }
    }

    private String normalizeAmount(String rawAmount) {
        String value = rawAmount.trim()
                .replace(",", "")
                .replace("_", "")
                .replace(" ", "");
        if (value.startsWith("(") && value.endsWith(")")) {
            value = "-" + value.substring(1, value.length() - 1);
        }
        value = value.replaceAll("[^0-9+\\-Ee.]", "");
        if (value.equals("-") || value.equals("+")) {
            return null;
        }
        return value;
    }
}
