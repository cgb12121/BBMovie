package bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto;

import java.util.List;

public record LedgerTimelineResponse(
        String paymentId,
        int totalEntries,
        List<LedgerEntryResponse> entries
) {
}
