package com.bbmovie.ai_assistant_service.core.low_level._assistant;

import com.bbmovie.ai_assistant_service.core.low_level._dto._response._ChatStreamChunk;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AiMode;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AssistantType;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface _Assistant {
    _AssistantType getType();
    Flux<_ChatStreamChunk> processMessage(
            UUID sessionId, String message, _AiMode aiMode, String userRole
    );
}
