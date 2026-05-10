package bbmovie.ai_platform.agentic_ai.dto.request;

import java.util.List;
import java.util.UUID;

public record DeleteArchivedSessionsDto(List<UUID> sessionIds) {}
