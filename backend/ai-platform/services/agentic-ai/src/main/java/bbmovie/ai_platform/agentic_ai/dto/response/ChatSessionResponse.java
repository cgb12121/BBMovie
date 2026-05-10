package bbmovie.ai_platform.agentic_ai.dto.response;

import java.util.UUID;
import java.time.Instant;

public record ChatSessionResponse(UUID id, String name, Instant createdAt, boolean archived) {}
