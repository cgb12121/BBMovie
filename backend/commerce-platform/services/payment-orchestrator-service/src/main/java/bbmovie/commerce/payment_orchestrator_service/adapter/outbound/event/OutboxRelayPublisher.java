package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.event;

import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.OutboxEventEntity;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.jpa.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayPublisher {

    private final OutboxEventRepository outboxEventRepository;
    @Qualifier("outboxKafkaTemplate")
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    @Value("${app.kafka.events.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${app.outbox.relay.enabled:true}")
    private boolean relayEnabled;

    @Value("${app.kafka.topic.payment-events:commerce.payment.events.v1}")
    private String topic;

    @Scheduled(fixedDelayString = "${app.outbox.relay.fixed-delay-ms:2000}")
    @Transactional
    public void relayBatch() {
        if (!relayEnabled || !kafkaEnabled) {
            return;
        }
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            log.warn("Outbox relay skipped: KafkaTemplate unavailable");
            return;
        }
        List<OutboxEventEntity> batch = outboxEventRepository
                .findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc("PENDING", Instant.now());
        for (OutboxEventEntity event : batch) {
            try {
                kafkaTemplate.send(topic, event.getPaymentId(), event.getPayloadJson());
                event.setStatus("SENT");
                event.setPublishedAt(Instant.now());
                event.setLastError(null);
            } catch (Exception ex) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(truncate(ex.getMessage(), 1900));
                event.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds(event.getAttempts())));
                log.warn("Outbox publish failed: id={}, attempts={}", event.getId(), event.getAttempts(), ex);
            }
        }
        outboxEventRepository.saveAll(batch);
    }

    private long backoffSeconds(int attempts) {
        return Math.min(60, Math.max(2, attempts * 2L));
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
