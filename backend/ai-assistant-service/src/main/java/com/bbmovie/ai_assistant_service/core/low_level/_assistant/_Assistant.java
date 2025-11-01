package com.bbmovie.ai_assistant_service.core.low_level._assistant;

import com.bbmovie.ai_assistant_service.core.low_level._entity._model.AssistantType;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface _Assistant {
    AssistantType getType();
    Flux<String> processMessage(UUID sessionId, String message, String userRole);
}
