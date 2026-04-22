package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.model;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record WebhookPayload(
        Map<String, String> headers,
        String rawBody,
        String contentType,
        Instant receivedAt
) {
    public Optional<String> header(String name) {
        if (headers == null || name == null) {
            return Optional.empty();
        }
        return headers.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}

