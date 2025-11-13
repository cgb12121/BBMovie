package com.bbmovie.ai_assistant_service.core.high_level.repository;

import com.bbmovie.ai_assistant_service.core.high_level.agent.domain.entity.ChatMessageEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChatMessageRepository extends R2dbcRepository<ChatMessageEntity, Long> {
    Flux<ChatMessageEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    Flux<Void> deleteBySessionId(String sessionId);
}