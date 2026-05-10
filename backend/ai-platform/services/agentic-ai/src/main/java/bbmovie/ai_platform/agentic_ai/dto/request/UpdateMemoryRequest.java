package bbmovie.ai_platform.agentic_ai.dto.request;

import java.util.UUID;

public record UpdateMemoryRequest(UUID memoryId, String newFact) {}

