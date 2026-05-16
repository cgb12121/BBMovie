package bbmovie.ai_platform.agentic_ai.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a single AI-persisted memory record.
 * {@code createdAt} is set by the system via {@link org.springframework.data.annotation.CreatedDate}.
 */
public record MemoryRecord(UUID id, String fact, Instant createdAt) {}
