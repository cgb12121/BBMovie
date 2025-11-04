package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatSession;
import com.example.common.dtos.CursorPageResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface _SessionService {
    Mono<CursorPageResponse<_ChatSession>> listSessionsWithCursor(UUID userId, String cursor, int size);

    Mono<_ChatSession> createSession(UUID userId, String sessionName);

    Mono<Void> deleteSession(UUID sessionId, UUID userId);

    Mono<_ChatSession> getAndValidateSessionOwnership(UUID sessionId, UUID userId);
}
