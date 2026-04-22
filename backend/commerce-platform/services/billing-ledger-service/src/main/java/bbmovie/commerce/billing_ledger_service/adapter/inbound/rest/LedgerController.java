package bbmovie.commerce.billing_ledger_service.adapter.inbound.rest;

import bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto.DashboardSummaryResponse;
import bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto.LedgerEntriesResponse;
import bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto.LedgerEntryResponse;
import bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto.LedgerTimelineResponse;
import bbmovie.commerce.billing_ledger_service.application.service.LedgerQueryService;
import bbmovie.commerce.billing_ledger_service.infrastructure.pdf.SimpleLedgerPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final LedgerQueryService ledgerQueryService;
    private final SimpleLedgerPdfGenerator pdfGenerator;

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public LedgerTimelineResponse getPaymentTimeline(@PathVariable String paymentId) {
        return ledgerQueryService.getTimeline(paymentId);
    }

    @GetMapping("/{paymentId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ResponseEntity<byte[]> getPaymentTimelinePdf(@PathVariable String paymentId) {
        List<LedgerEntryResponse> entries = ledgerQueryService.getEntriesForPdf(paymentId);
        byte[] pdf = pdfGenerator.generate(paymentId, entries);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("ledger-" + paymentId + ".pdf")
                                .build()
                                .toString()
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','SUPPORT')")
    public DashboardSummaryResponse getSummary(
            @RequestParam(name = "recentLimit", defaultValue = "20") int recentLimit
    ) {
        return ledgerQueryService.getSummary(recentLimit);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public LedgerEntriesResponse getByUserId(@PathVariable String userId) {
        return ledgerQueryService.getByUserId(userId);
    }

    @GetMapping("/subscription/{subscriptionId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public LedgerEntriesResponse getBySubscriptionId(@PathVariable String subscriptionId) {
        return ledgerQueryService.getBySubscriptionId(subscriptionId);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','SUPPORT')")
    public LedgerEntriesResponse search(
            @RequestParam(name = "provider", required = false) String provider,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(name = "subscriptionId", required = false) String subscriptionId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        Instant fromTs = parseInstantOrNull(from, "from");
        Instant toTs = parseInstantOrNull(to, "to");
        return ledgerQueryService.search(provider, status, userId, subscriptionId, fromTs, toTs, limit);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    public ResponseEntity<StreamingResponseBody> exportCsv(
            @RequestParam(name = "month") String month
    ) {
        YearMonth yearMonth = parseYearMonth(month);
        List<LedgerEntryResponse> entries = ledgerQueryService.getEntriesForMonth(yearMonth);
        StreamingResponseBody body = outputStream -> writeCsv(entries, outputStream);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("ledger-" + month + ".csv")
                                .build()
                                .toString()
                )
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    private Instant parseInstantOrNull(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid " + fieldName + " timestamp. Use ISO-8601 format.");
        }
    }

    private YearMonth parseYearMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid month format. Use yyyy-MM, e.g. 2026-04.");
        }
    }

    private void writeCsv(List<LedgerEntryResponse> entries, java.io.OutputStream outputStream) throws java.io.IOException {
        outputStream.write("id,paymentId,eventId,entryType,provider,status,amount,currency,externalReferenceId,userId,userEmail,purpose,subscriptionId,subscriptionCampaignId,occurredAt\n".getBytes(StandardCharsets.UTF_8));
        for (LedgerEntryResponse entry : entries) {
            String line = csv(entry.id())
                    + "," + csv(entry.paymentId())
                    + "," + csv(entry.eventId())
                    + "," + csv(entry.entryType())
                    + "," + csv(entry.provider())
                    + "," + csv(entry.status())
                    + "," + csv(entry.amount())
                    + "," + csv(entry.currency())
                    + "," + csv(entry.externalReferenceId())
                    + "," + csv(entry.userId())
                    + "," + csv(entry.userEmail())
                    + "," + csv(entry.purpose())
                    + "," + csv(entry.subscriptionId())
                    + "," + csv(entry.subscriptionCampaignId())
                    + "," + csv(entry.occurredAt())
                    + "\n";
            outputStream.write(line.getBytes(StandardCharsets.UTF_8));
        }
        outputStream.flush();
    }

    private String csv(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
