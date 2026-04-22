package bbmovie.commerce.billing_ledger_service.infrastructure.pdf;

import bbmovie.commerce.billing_ledger_service.adapter.inbound.rest.dto.LedgerEntryResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class SimpleLedgerPdfGenerator {
    private static final float MARGIN = 40f;
    private static final float FONT_SIZE = 9f;
    private static final float LEADING = 13f;

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
        return buildPdf(lines);
    }

    private byte[] buildPdf(List<String> lines) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, page.getMediaBox().getHeight() - MARGIN);

            float cursorY = page.getMediaBox().getHeight() - MARGIN;
            for (String line : lines) {
                if (cursorY <= MARGIN) {
                    contentStream.endText();
                    contentStream.close();

                    page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN, page.getMediaBox().getHeight() - MARGIN);
                    cursorY = page.getMediaBox().getHeight() - MARGIN;
                }
                contentStream.showText(sanitizeLine(line));
                contentStream.newLineAtOffset(0, -LEADING);
                cursorY -= LEADING;
            }

            contentStream.endText();
            contentStream.close();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate ledger PDF", ex);
        }
    }

    private String sanitizeLine(String text) {
        String safe = text == null ? "" : text;
        return safe.replace('\t', ' ');
    }

    private String nullSafe(String value) {
        return value == null ? "N/A" : value;
    }
}
