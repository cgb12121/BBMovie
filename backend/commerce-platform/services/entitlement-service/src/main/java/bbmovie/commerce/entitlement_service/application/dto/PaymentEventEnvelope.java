package bbmovie.commerce.entitlement_service.application.dto;

import java.util.Map;

public record PaymentEventEnvelope(
        String eventType,
        String paymentId,
        Map<String, Object> payload
) {
}
        