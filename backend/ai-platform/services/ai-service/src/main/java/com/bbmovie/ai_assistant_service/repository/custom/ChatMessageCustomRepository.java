package com.bbmovie.ai_assistant_service.repository.custom;

import com.bbmovie.ai_assistant_service.entity.ChatMessage;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

public interface ChatMessageCustomRepository {
    Flux<ChatMessage> getWithCursor(UUID sessionId, Instant cursorTime, int size);
}
