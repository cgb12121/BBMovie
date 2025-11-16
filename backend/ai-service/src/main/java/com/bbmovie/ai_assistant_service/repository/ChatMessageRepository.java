package com.bbmovie.ai_assistant_service.repository;

import com.bbmovie.ai_assistant_service.entity.ChatMessage;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository("_ChatMessageRepository")
public interface ChatMessageRepository extends ChatMessageCustomRepository, R2dbcRepository<ChatMessage, Long> {
    Mono<Void> deleteBySessionId(UUID id);
}