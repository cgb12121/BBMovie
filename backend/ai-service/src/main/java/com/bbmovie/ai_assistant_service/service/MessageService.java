package com.bbmovie.ai_assistant_service.service;

import com.bbmovie.ai_assistant_service.entity.ChatMessage;
import com.bbmovie.common.dtos.CursorPageResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface MessageService {
    Mono<ChatMessage> saveUserMessage(UUID sessionId, String message);

    Mono<ChatMessage> saveAiResponse(UUID sessionId, String content);

    Mono<CursorPageResponse<ChatMessage>> getMessagesWithCursor(UUID sessionId, UUID userId, String cursor, int size);
}
