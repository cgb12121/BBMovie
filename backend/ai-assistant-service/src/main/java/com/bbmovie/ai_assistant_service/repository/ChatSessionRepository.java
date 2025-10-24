package com.bbmovie.ai_assistant_service.repository;

import com.bbmovie.ai_assistant_service.domain.entity.ChatSession;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ChatSessionRepository extends R2dbcRepository<ChatSession, Long> {
    Mono<ChatSession> findByUserId(String userId);
    Mono<ChatSession> findBySessionName(String sessionName);
}