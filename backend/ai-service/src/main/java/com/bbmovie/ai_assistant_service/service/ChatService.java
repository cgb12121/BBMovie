package com.bbmovie.ai_assistant_service.service;

import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ChatService {
    Flux<ChatStreamChunk> chat(UUID sessionId, ChatRequestDto request, Jwt jwt);
}