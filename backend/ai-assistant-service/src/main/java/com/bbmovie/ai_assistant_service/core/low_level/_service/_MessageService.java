package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatMessage;
import com.example.common.dtos.CursorPageResponse;
import dev.langchain4j.data.message.ChatMessage;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface _MessageService {
    Mono<_ChatMessage> saveUserMessage(UUID sessionId, String message);

    Mono<_ChatMessage> saveAiResponse(UUID sessionId, String content);

    Mono<CursorPageResponse<_ChatMessage>> getMessagesWithCursor(UUID sessionId, UUID userId, String cursor, int size);

    List<ChatMessage> loadRecentMessages(UUID sessionId, int numMessages);
}
