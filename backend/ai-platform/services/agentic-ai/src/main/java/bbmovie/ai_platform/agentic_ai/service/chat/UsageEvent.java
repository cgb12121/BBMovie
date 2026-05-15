package bbmovie.ai_platform.agentic_ai.service.chat;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Event representing AI token usage for a specific request.
 * Published to NATS for asynchronous processing and persistence.
 */
public record UsageEvent(
    UUID userId,
    UUID sessionId,
    String model,
    long promptTokens,
    long completionTokens,
    long totalTokens,
    Instant timestamp
) implements Serializable {
    
    public static UsageEvent of(UUID userId, UUID sessionId, String model, long prompt, long completion) {
        return new UsageEvent(
            userId, 
            sessionId, 
            model, 
            prompt, 
            completion, 
            prompt + completion, 
            Instant.now()
        );
    }
}
