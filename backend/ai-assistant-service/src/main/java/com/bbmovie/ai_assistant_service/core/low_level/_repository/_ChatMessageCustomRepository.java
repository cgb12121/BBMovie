package com.bbmovie.ai_assistant_service.core.low_level._repository;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatMessage;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

public interface _ChatMessageCustomRepository {
    Flux<_ChatMessage> getWithCursor(UUID sessionId, Instant cursorTime, int size);
}
