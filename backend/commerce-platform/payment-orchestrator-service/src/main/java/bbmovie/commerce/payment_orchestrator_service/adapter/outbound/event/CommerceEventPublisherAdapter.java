package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.event;

import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.event.EventPublisherPort;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class CommerceEventPublisherAdapter implements EventPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String topic;

    public CommerceEventPublisherAdapter(
            ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider,
            ObjectMapper objectMapper,
            @Value("${app.kafka.events.enabled:false}") boolean enabled,
            @Value("${app.kafka.topic.payment-events:commerce.payment.events.v1}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.topic = topic;
    }

    @Override
    public void publish(String eventType, String paymentId, Map<String, Object> payload) {
        if (!enabled) {
            log.debug("Kafka event publishing skipped because disabled: eventType={}, paymentId={}", eventType, paymentId);
            return;
        }
        if (kafkaTemplate == null) {
            log.warn("Kafka event publishing skipped because KafkaTemplate is unavailable: eventType={}, paymentId={}", eventType, paymentId);
            return;
        }
        try {
            Map<String, Object> event = Map.of(
                    "eventType", eventType,
                    "paymentId", paymentId,
                    "payload", payload
            );
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, paymentId, json);
            log.info("Kafka event published: topic={}, eventType={}, paymentId={}", topic, eventType, paymentId);
        } catch (Exception e) {
            log.warn("Failed to publish Kafka event {}", eventType, e);
        }
    }
}

