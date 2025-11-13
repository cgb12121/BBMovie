package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatMessage;
import com.bbmovie.common.dtos.CursorPageResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface _MessageService {
    Mono<_ChatMessage> saveUserMessage(UUID sessionId, String message);

    Mono<_ChatMessage> saveAiResponse(UUID sessionId, String content);

    Mono<CursorPageResponse<_ChatMessage>> getMessagesWithCursor(UUID sessionId, UUID userId, String cursor, int size);
}
