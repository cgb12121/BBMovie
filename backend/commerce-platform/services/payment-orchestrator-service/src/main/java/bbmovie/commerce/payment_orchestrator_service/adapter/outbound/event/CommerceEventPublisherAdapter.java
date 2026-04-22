package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.event;

import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.event.EventPublisherPort;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.OutboxEventEntity;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.OutboxStatus;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.jpa.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
public class CommerceEventPublisherAdapter implements EventPublisherPort {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public CommerceEventPublisherAdapter(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            @Value("${app.kafka.events.enabled:false}") boolean enabled
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void publish(String eventType, String paymentId, Map<String, Object> payload) {
        if (!enabled) {
            log.debug("Event skipped because publishing disabled: eventType={}, paymentId={}", eventType, paymentId);
            return;
        }
        try {
            Map<String, Object> event = Map.of(
                    "eventType", eventType,
                    "paymentId", paymentId,
                    "payload", payload
            );
            String json = objectMapper.writeValueAsString(event);

            OutboxEventEntity row = new OutboxEventEntity();
            row.setEventType(eventType);
            row.setPaymentId(paymentId);
            row.setPayloadJson(json);
            row.setStatus(OutboxStatus.PENDING);
            row.setAttempts(0);
            row.setCreatedAt(Instant.now());
            row.setNextAttemptAt(Instant.now());
            outboxEventRepository.save(row);
            log.info("Outbox event queued: eventType={}, paymentId={}, outboxId={}", eventType, paymentId, row.getId());
        } catch (Exception e) {
            log.error("Failed to enqueue outbox event: eventType={}, paymentId={}", eventType, paymentId, e);
            throw new IllegalStateException(
                    "Cannot persist outbox event for eventType=%s, paymentId=%s".formatted(eventType, paymentId),
                    e
            );
        }
    }
}

