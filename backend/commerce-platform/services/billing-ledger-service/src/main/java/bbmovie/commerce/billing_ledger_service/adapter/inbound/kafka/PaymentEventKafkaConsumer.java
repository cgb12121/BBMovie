package bbmovie.commerce.billing_ledger_service.adapter.inbound.kafka;

import bbmovie.commerce.billing_ledger_service.application.dto.PaymentEventEnvelope;
import bbmovie.commerce.billing_ledger_service.application.service.PaymentLedgerIngestionService;
import bbmovie.commerce.commerce_common.crypto.Sha256Hasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventKafkaConsumer {

    private final PaymentLedgerIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topic.payment-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(
            String rawEvent,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = "eventId", required = false) String eventIdHeader,
            @Header(name = "ce_id", required = false) String cloudEventId
    ) {
        try {
            PaymentEventEnvelope envelope = objectMapper.readValue(rawEvent, PaymentEventEnvelope.class);
            String eventId = resolveEventId(eventIdHeader, cloudEventId, rawEvent);
            ingestionService.ingest(eventId, envelope);
            log.info(
                    "Payment event ingested: eventId={}, eventType={}, paymentId={}, key={}, offset={}",
                    eventId,
                    envelope.eventType(),
                    envelope.paymentId(),
                    key,
                    offset
            );
        } catch (Exception ex) {
            log.error("Failed to ingest payment event: key={}, offset={}", key, offset, ex);
            throw new IllegalStateException("Unable to process payment event", ex);
        }
    }

    private String resolveEventId(String eventIdHeader, String cloudEventId, String rawEvent) {
        if (eventIdHeader != null && !eventIdHeader.isBlank()) {
            return eventIdHeader;
        }
        if (cloudEventId != null && !cloudEventId.isBlank()) {
            return cloudEventId;
        }
        return "sha256:" + Sha256Hasher.sha256Hex(rawEvent);
    }
}
