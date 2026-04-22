package bbmovie.commerce.subscription_service.adapter.inbound.kafka;

import bbmovie.commerce.commerce_common.crypto.Sha256Hasher;
import bbmovie.commerce.subscription_service.application.dto.PaymentEventEnvelope;
import bbmovie.commerce.subscription_service.application.service.SubscriptionLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventKafkaConsumer {

    private final SubscriptionLifecycleService lifecycleService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topic.payment-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(
            String rawEvent,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String eventIdHeader,
            @Header(value = "ce_id", required = false) String cloudEventId
    ) {
        try {
            PaymentEventEnvelope envelope = objectMapper.readValue(rawEvent, PaymentEventEnvelope.class);
            String eventId = resolveEventId(eventIdHeader, cloudEventId, rawEvent);
            lifecycleService.ingest(eventId, envelope);
            log.info(
                    "Subscription event ingested: eventId={}, eventType={}, paymentId={}",
                    eventId,
                    envelope.eventType(),
                    envelope.paymentId()
            );
        } catch (Exception ex) {
            log.error("Failed to process payment event for subscription-service", ex);
            throw new IllegalStateException("Subscription event ingestion failed", ex);
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
