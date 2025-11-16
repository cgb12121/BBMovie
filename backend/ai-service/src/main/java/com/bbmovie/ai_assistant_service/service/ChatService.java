package com.bbmovie.ai_assistant_service.service;

import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import com.bbmovie.ai_assistant_service.entity.model.AssistantType;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ChatService {
    Flux<ChatStreamChunk> chat(
            UUID sessionId, String message, AiMode aiMode, AssistantType assistantType, Jwt jwt
    );
}