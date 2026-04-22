package bbmovie.commerce.billing_ledger_service.application.service;

import bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto.DashboardSummaryResponse;
import bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto.LedgerEntriesResponse;
import bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto.LedgerEntryResponse;
import bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto.LedgerTimelineResponse;
import bbmovie.commerce.billing_ledger_service.infrastructure.persistence.entity.LedgerEntryEntity;
import bbmovie.commerce.billing_ledger_service.infrastructure.persistence.repo.LedgerEntryRepository;
import bbmovie.commerce.billing_ledger_service.infrastructure.persistence.repo.PaymentEventInboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LedgerQueryService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final PaymentEventInboxRepository paymentEventInboxRepository;

    public LedgerTimelineResponse getTimeline(String paymentId) {
        List<LedgerEntryResponse> entries = ledgerEntryRepository.findByPaymentIdOrderByOccurredAtAscIdAsc(paymentId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new LedgerTimelineResponse(paymentId, entries.size(), entries);
    }

    public DashboardSummaryResponse getSummary(int recentLimit) {
        int safeLimit = Math.max(1, Math.min(recentLimit, 100));
        List<LedgerEntryResponse> recentEntries = ledgerEntryRepository
                .findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toResponse)
                .toList();

        return new DashboardSummaryResponse(
                paymentEventInboxRepository.count(),
                ledgerEntryRepository.count(),
                ledgerEntryRepository.countByOccurredAtAfter(Instant.now().minus(24, ChronoUnit.HOURS)),
                toCountMap(ledgerEntryRepository.countByProvider()),
                toCountMap(ledgerEntryRepository.countByStatus()),
                toCountMap(ledgerEntryRepository.countByEntryType()),
                recentEntries
        );
    }

    public List<LedgerEntryResponse> getEntriesForPdf(String paymentId) {
        return ledgerEntryRepository.findByPaymentIdOrderByOccurredAtAscIdAsc(paymentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LedgerEntriesResponse getByUserId(String userId) {
        List<LedgerEntryResponse> entries = ledgerEntryRepository.findByUserIdOrderByOccurredAtDescIdDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new LedgerEntriesResponse("userId=" + userId, entries.size(), entries);
    }

    public LedgerEntriesResponse getBySubscriptionId(String subscriptionId) {
        List<LedgerEntryResponse> entries = ledgerEntryRepository.findBySubscriptionIdOrderByOccurredAtDescIdDesc(subscriptionId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new LedgerEntriesResponse("subscriptionId=" + subscriptionId, entries.size(), entries);
    }

    public LedgerEntriesResponse search(
            String provider,
            String status,
            String userId,
            String subscriptionId,
            Instant from,
            Instant to,
            int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        List<LedgerEntryResponse> entries = ledgerEntryRepository.search(
                        blankToNull(provider),
                        blankToNull(status),
                        blankToNull(userId),
                        blankToNull(subscriptionId),
                        from,
                        to,
                        PageRequest.of(0, safeLimit)
                )
                .stream()
                .map(this::toResponse)
                .toList();

        String query = "provider=%s,status=%s,userId=%s,subscriptionId=%s,from=%s,to=%s,limit=%d".formatted(
                provider, status, userId, subscriptionId, from, to, safeLimit
        );
        return new LedgerEntriesResponse(query, entries.size(), entries);
    }

    public List<LedgerEntryResponse> getEntriesForMonth(YearMonth month) {
        Instant from = month.atDay(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC).minusMillis(1);
        return ledgerEntryRepository.findByOccurredAtBetweenOrderByOccurredAtAscIdAsc(from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private LedgerEntryResponse toResponse(LedgerEntryEntity entity) {
        return new LedgerEntryResponse(
                entity.getId(),
                entity.getPaymentId(),
                entity.getEventId(),
                entity.getEntryType(),
                entity.getProvider(),
                entity.getStatus(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getExternalReferenceId(),
                entity.getUserId(),
                entity.getUserEmail(),
                entity.getPurpose(),
                entity.getSubscriptionId(),
                entity.getSubscriptionCampaignId(),
                entity.getPayloadJson(),
                entity.getOccurredAt()
        );
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = String.valueOf(row[0]);
            Long value = ((Number) row[1]).longValue();
            out.put(key, value);
        }
        return out;
    }
}
