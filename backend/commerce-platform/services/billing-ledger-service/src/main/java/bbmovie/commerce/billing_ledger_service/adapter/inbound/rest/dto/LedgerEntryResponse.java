package bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto;

import bbmovie.commerce.billing_ledger_service.domain.LedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(
        Long id,
        String paymentId,
        String eventId,
        LedgerEntryType entryType,
        String provider,
        String status,
        BigDecimal amount,
        String currency,
        String externalReferenceId,
        String userId,
        String userEmail,
        String purpose,
        String subscriptionId,
        String subscriptionCampaignId,
        String payloadJson,
        Instant occurredAt
) {
}
