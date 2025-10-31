package com.bbmovie.ai_assistant_service.core.low_level._assistant;

import com.bbmovie.ai_assistant_service.core.low_level._model.AssistantMetadata;
import com.bbmovie.ai_assistant_service.core.low_level._model.AssistantType;
import reactor.core.publisher.Flux;

public interface _Assistant {

    AssistantType getType();

    Flux<String> processMessage(String sessionId, String message, String userRole);

    AssistantMetadata getMetadata();
}
