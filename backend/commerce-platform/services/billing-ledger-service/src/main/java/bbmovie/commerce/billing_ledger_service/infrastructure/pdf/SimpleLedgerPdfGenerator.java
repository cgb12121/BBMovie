package bbmovie.commerce.billing_ledger_service.infrastructure.pdf;

import bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto.LedgerEntryResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class SimpleLedgerPdfGenerator {

    public byte[] generate(String paymentId, List<LedgerEntryResponse> entries) {
        List<String> lines = new ArrayList<>();
        lines.add("Billing Ledger Report");
        lines.add("Payment ID: " + paymentId);
        lines.add("Generated at: " + Instant.now());
        lines.add("Total entries: " + entries.size());
        lines.add("------------------------------------------------------------");
        for (LedgerEntryResponse e : entries) {
            lines.add(
                    String.format(
                            "#%d | %s | %s | provider=%s | status=%s | sub=%s | campaign=%s",
                            e.id(),
                            e.occurredAt(),
                            e.entryType(),
                            nullSafe(e.provider()),
                            nullSafe(e.status()),
                            nullSafe(e.subscriptionId()),
                            nullSafe(e.subscriptionCampaignId())
                    )
            );
        }
        return buildMinimalPdf(lines);
    }

    private byte[] buildMinimalPdf(List<String> lines) {
        StringBuilder text = new StringBuilder();
        text.append("BT\n/F1 10 Tf\n50 780 Td\n");
        for (int i = 0; i < lines.size(); i++) {
            String escaped = escapePdfText(lines.get(i));
            if (i == 0) {
                text.append("(").append(escaped).append(") Tj\n");
            } else {
                text.append("0 -14 Td (").append(escaped).append(") Tj\n");
            }
        }
        text.append("ET\n");

        String stream = text.toString();
        String obj1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n";
        String obj2 = "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n";
        String obj3 = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n";
        String obj4 = "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n";
        String obj5 = "5 0 obj\n<< /Length " + stream.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n"
                + stream + "endstream\nendobj\n";

        String body = obj1 + obj2 + obj3 + obj4 + obj5;
        List<Integer> offsets = new ArrayList<>();
        String header = "%PDF-1.4\n";
        int cursor = header.getBytes(StandardCharsets.US_ASCII).length;
        offsets.add(cursor);
        cursor += obj1.getBytes(StandardCharsets.US_ASCII).length;
        offsets.add(cursor);
        cursor += obj2.getBytes(StandardCharsets.US_ASCII).length;
        offsets.add(cursor);
        cursor += obj3.getBytes(StandardCharsets.US_ASCII).length;
        offsets.add(cursor);
        cursor += obj4.getBytes(StandardCharsets.US_ASCII).length;
        offsets.add(cursor);
        cursor += obj5.getBytes(StandardCharsets.US_ASCII).length;
        int xrefPos = cursor;

        StringBuilder xref = new StringBuilder();
        xref.append("xref\n0 6\n");
        xref.append("0000000000 65535 f \n");
        for (int offset : offsets) {
            xref.append(String.format("%010d 00000 n %n", offset));
        }
        xref.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n")
                .append(xrefPos)
                .append("\n%%EOF\n");

        return (header + body + xref).getBytes(StandardCharsets.US_ASCII);
    }

    private String escapePdfText(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String nullSafe(String value) {
        return value == null ? "N/A" : value;
    }
}
