package bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto;

import java.util.List;

public record LedgerEntriesResponse(
        String query,
        int totalEntries,
        List<LedgerEntryResponse> entries
) {
}
