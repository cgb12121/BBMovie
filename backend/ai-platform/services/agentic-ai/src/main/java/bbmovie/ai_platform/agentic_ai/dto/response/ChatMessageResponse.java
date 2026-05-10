package bbmovie.ai_platform.agentic_ai.dto.response;

import java.util.UUID;
import java.time.Instant;

public record ChatMessageResponse(UUID id, UUID sessionId, String content, String sender, UUID parentId, Instant timestamp) {}
