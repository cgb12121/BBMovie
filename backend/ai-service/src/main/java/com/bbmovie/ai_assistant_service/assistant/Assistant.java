package com.bbmovie.ai_assistant_service.assistant;

import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import com.bbmovie.ai_assistant_service.entity.model.AssistantType;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface Assistant {
    AssistantType getType();
    Flux<ChatStreamChunk> processMessage(UUID sessionId, String message, AiMode aiMode, String userRole);
}
