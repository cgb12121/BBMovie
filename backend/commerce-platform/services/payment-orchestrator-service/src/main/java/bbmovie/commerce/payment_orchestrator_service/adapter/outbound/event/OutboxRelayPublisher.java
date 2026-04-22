package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.event;

import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.OutboxEventEntity;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.OutboxStatus;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.jpa.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayPublisher {

    private static final int RELAY_BATCH_SIZE = 100;
    private static final long KAFKA_SEND_TIMEOUT_SECONDS = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final TransactionTemplate transactionTemplate;
    @Qualifier("outboxKafkaTemplate")
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    @Value("${app.kafka.events.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${app.outbox.relay.enabled:true}")
    private boolean relayEnabled;

    @Value("${app.kafka.topic.payment-events:commerce.payment.events.v1}")
    private String topic;

    @Scheduled(fixedDelayString = "${app.outbox.relay.fixed-delay-ms:2000}")
    public void relayBatch() {
        if (!relayEnabled || !kafkaEnabled) {
            return;
        }
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            log.warn("Outbox relay skipped: KafkaTemplate unavailable");
            return;
        }

        for (int i = 0; i < RELAY_BATCH_SIZE; i++) {
            Boolean processed = transactionTemplate.execute(status -> {
                // Claim a single row with FOR UPDATE SKIP LOCKED to avoid cross-instance duplicates.
                List<OutboxEventEntity> claimed = outboxEventRepository
                        .claimPendingBatch(OutboxStatus.PENDING.name(), Instant.now(), 1);
                if (claimed.isEmpty()) {
                    return false;
                }

                OutboxEventEntity event = claimed.get(0);
                try {
                    kafkaTemplate.send(topic, event.getPaymentId(), event.getPayloadJson())
                            .get(KAFKA_SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    event.setStatus(OutboxStatus.SENT);
                    event.setPublishedAt(Instant.now());
                    event.setLastError(null);
                } catch (Exception ex) {
                    if (ex instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    event.setAttempts(event.getAttempts() + 1);
                    event.setLastError(truncate(ex.getMessage(), 1900));
                    event.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds(event.getAttempts())));
                    log.warn("Outbox publish failed: id={}, attempts={}", event.getId(), event.getAttempts(), ex);
                }

                outboxEventRepository.save(event);
                return true;
            });

            if (!Boolean.TRUE.equals(processed)) {
                break;
            }
        }
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
