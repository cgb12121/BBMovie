package com.bbmovie.ai_assistant_service.core.low_level._repository;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatSession;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository("_ChatSessionRepository")
public interface _ChatSessionRepository extends R2dbcRepository<_ChatSession, UUID> {
    Flux<_ChatSession> findByUserId(UUID userId);
}
