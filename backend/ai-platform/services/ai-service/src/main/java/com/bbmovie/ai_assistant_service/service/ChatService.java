package com.bbmovie.ai_assistant_service.service;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;

import reactor.core.publisher.Flux;

public interface ChatService {
    
    /**
     * Unified chat processing method.
     * Handles both text-only and messages with file attachments (already uploaded).
     * 
     * @param sessionId Chat session ID
     * @param request Chat request containing message and optional attachments
     * @param jwt User authentication token
     * @return Streaming chat response
     */
    Flux<ChatStreamChunk> chat(UUID sessionId, ChatRequestDto request, Jwt jwt);
}
