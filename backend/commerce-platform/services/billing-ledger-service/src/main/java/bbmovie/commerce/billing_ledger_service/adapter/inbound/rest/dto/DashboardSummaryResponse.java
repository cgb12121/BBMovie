package bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto;

import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
        long totalInboxEvents,
        long totalLedgerEntries,
        long ledgerEntriesLast24h,
        Map<String, Long> entriesByProvider,
        Map<String, Long> entriesByStatus,
        Map<String, Long> entriesByType,
        List<LedgerEntryResponse> recentEntries
) {
}
