package bbmovie.ai_platform.agentic_ai.repository;

import bbmovie.ai_platform.agentic_ai.entity.ChatSession;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface SessionRepository extends R2dbcRepository<ChatSession, UUID> {

    @Query("SELECT * FROM chat_session WHERE user_id = :userId AND is_archived = false ORDER BY created_at DESC LIMIT :limit")
    Flux<ChatSession> findActiveSessions(UUID userId, int limit);

    @Query("SELECT * FROM chat_session WHERE user_id = :userId AND is_archived = false AND created_at < :cursor ORDER BY created_at DESC LIMIT :limit")
    Flux<ChatSession> findActiveSessionsWithCursor(UUID userId, Instant cursor, int limit);

    @Query("SELECT * FROM chat_session WHERE user_id = :userId AND is_archived = true ORDER BY created_at DESC LIMIT :limit")
    Flux<ChatSession> findArchivedSessions(UUID userId, int limit);

    @Query("SELECT * FROM chat_session WHERE user_id = :userId AND is_archived = true AND created_at < :cursor ORDER BY created_at DESC LIMIT :limit")
    Flux<ChatSession> findArchivedSessionsWithCursor(UUID userId, Instant cursor, int limit);
}
