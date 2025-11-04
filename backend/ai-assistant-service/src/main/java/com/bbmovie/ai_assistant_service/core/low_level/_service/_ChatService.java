package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._dto._ChatStreamChunk;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AssistantType;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface _ChatService {
    Flux<_ChatStreamChunk> chat(UUID sessionId, String message, _AssistantType assistantType, Jwt jwt);
}
