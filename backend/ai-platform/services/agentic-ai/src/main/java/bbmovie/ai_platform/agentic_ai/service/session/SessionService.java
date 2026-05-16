package bbmovie.ai_platform.agentic_ai.service.session;

import bbmovie.ai_platform.agentic_ai.dto.response.ChatSessionResponse;
import com.bbmovie.common.dtos.CursorPageResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Contract for session lifecycle management.
 *
 * <p>Implementations are expected to enforce ownership checks (via {@code @CheckOwnership})
 * and integrate with the chat memory system on deletion.
 */
public interface SessionService {

    Mono<CursorPageResponse<ChatSessionResponse>> activeSessionsWithCursor(UUID userId, String cursor, int size);

    Mono<ChatSessionResponse> createSession(UUID userId, String sessionName);

    Mono<Void> deleteSession(UUID sessionId, UUID userId);

    Mono<ChatSessionResponse> updateSessionName(UUID sessionId, UUID userId, String newName);

    Mono<ChatSessionResponse> setSessionArchived(UUID sessionId, UUID userId, boolean archived);

    Mono<CursorPageResponse<ChatSessionResponse>> listArchivedSessions(UUID userId, String cursor, int size);

    Mono<Void> deleteArchivedSessions(List<UUID> sessionIds, UUID userId);
}
