package bbmovie.ai_platform.agentic_ai.repository;

import bbmovie.ai_platform.agentic_ai.entity.ChatMessage;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface MessageRepository extends R2dbcRepository<ChatMessage, UUID> {

    @Query("SELECT * FROM chat_message WHERE session_id = :sessionId AND user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    Flux<ChatMessage> findBySessionId(UUID sessionId, UUID userId, int limit);

    @Query("SELECT * FROM chat_message WHERE session_id = :sessionId AND user_id = :userId AND created_at < :cursor ORDER BY created_at DESC LIMIT :limit")
    Flux<ChatMessage> findBySessionIdWithCursor(UUID sessionId, UUID userId, Instant cursor, int limit);

    Flux<ChatMessage> findAllBySessionIdOrderByCreatedAtDesc(UUID sessionId);

    Flux<ChatMessage> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    /**
     * Fetches the most recent {@code limit} messages for a session directly at the DB level.
     * Returns rows in DESC order (newest first) — caller must reverse if ASC order is needed.
     * Use this instead of {@code findAllBySessionIdOrderByCreatedAtAsc + takeLast()}
     * to avoid fetching the entire message history into memory.
     */
    @Query("SELECT * FROM chat_message WHERE session_id = :sessionId ORDER BY created_at DESC LIMIT :limit")
    Flux<ChatMessage> findLastNBySessionId(UUID sessionId, int limit);

    Mono<Void> deleteAllBySessionId(UUID fromString);
}
