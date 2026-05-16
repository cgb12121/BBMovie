package bbmovie.ai_platform.agentic_ai.repository;

import bbmovie.ai_platform.agentic_ai.entity.AgentMemory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface MemoryRepository extends R2dbcRepository<AgentMemory, UUID> {

    /**
     * Returns all memories for a user, most recent first.
     * Used by the user-facing list endpoint and by the personalization context builder.
     */
    Flux<AgentMemory> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Returns the count of memories for a user.
     * Used by {@code saveMemory} tool to enforce the per-user cap before persisting.
     */
    Mono<Long> countByUserId(UUID userId);
}
