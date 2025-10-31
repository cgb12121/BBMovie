package com.bbmovie.ai_assistant_service.core.low_level._repository;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatMessage;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository("_ChatMessageRepository")
public interface _ChatMessageRepository extends R2dbcRepository<_ChatMessage, Long> {
    Flux<Object> deleteBySessionId(UUID id);
}
