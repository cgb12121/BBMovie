package bbmovie.commerce.payment_orchestrator_service.application.port.outbound.event;

import java.util.Map;

public interface EventPublisherPort {
    void publish(String eventType, String paymentId, Map<String, Object> payload);
}

