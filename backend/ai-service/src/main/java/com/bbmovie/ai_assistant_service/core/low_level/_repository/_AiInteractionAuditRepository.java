package com.bbmovie.ai_assistant_service.core.low_level._repository;

import com.bbmovie.ai_assistant_service.core.low_level._entity._AiInteractionAudit;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface _AiInteractionAuditRepository extends ReactiveCrudRepository<_AiInteractionAudit, Long> {

    @Query("""
        SELECT * FROM ai_interaction_audit WHERE 
        (:interactionType IS NULL OR interaction_type = :interactionType) AND 
        (:sessionId IS NULL OR session_id = :sessionId) AND
        (:startDate IS NULL OR timestamp >= :startDate) AND 
        (:endDate IS NULL OR timestamp <= :endDate) 
        ORDER BY timestamp DESC
    """)
    Flux<_AiInteractionAudit> findByCriteria(
            _InteractionType interactionType,
            UUID sessionId,
            Instant startDate,
            Instant endDate
    );

    @Query("SELECT COALESCE(SUM(prompt_tokens), 0) FROM ai_interaction_audit")
    Mono<Long> sumTotalPromptTokens();

    @Query("SELECT COALESCE(SUM(response_tokens), 0) FROM ai_interaction_audit")
    Mono<Long> sumTotalResponseTokens();

}
