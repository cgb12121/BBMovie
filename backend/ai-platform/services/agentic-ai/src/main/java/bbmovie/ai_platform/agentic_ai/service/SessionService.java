package bbmovie.ai_platform.agentic_ai.service;

import bbmovie.ai_platform.agentic_ai.dto.response.ChatSessionResponse;
import bbmovie.ai_platform.agentic_ai.entity.ChatSession;
import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import bbmovie.ai_platform.agentic_ai.repository.SessionRepository;
import bbmovie.ai_platform.aop_policy.annotation.CheckOwnership;
import com.bbmovie.common.dtos.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final ChatMemory chatMemory;

    public Mono<CursorPageResponse<ChatSessionResponse>> activeSessionsWithCursor(UUID userId, String cursor, int size) {
        Flux<ChatSession> sessionFlux;
        if (cursor == null || cursor.isBlank()) {
            sessionFlux = sessionRepository.findActiveSessions(userId, size + 1);
        } else {
            Instant cursorTime = Instant.parse(cursor);
            sessionFlux = sessionRepository.findActiveSessionsWithCursor(userId, cursorTime, size + 1);
        }

        return sessionFlux.collectList().map(sessions -> {
            boolean hasNext = sessions.size() > size;
            List<ChatSession> pagedSessions = hasNext ? sessions.subList(0, size) : sessions;
            String nextCursor = pagedSessions.isEmpty() ? null : pagedSessions.get(pagedSessions.size() - 1).getCreatedAt().toString();
            
            List<ChatSessionResponse> responses = pagedSessions.stream()
                    .map(s -> new ChatSessionResponse(s.getId(), s.getName(), s.getCreatedAt(), s.isArchived()))
                    .toList();
            return new CursorPageResponse<>(responses, nextCursor, hasNext, size);
        });
    }

    public Mono<ChatSessionResponse> createSession(UUID userId, String sessionName) {
        ChatSession session = ChatSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name(sessionName)
                .isArchived(false)
                .build()
                .asNew();
                
        return sessionRepository.save(session)
                .map(s -> new ChatSessionResponse(s.getId(), s.getName(), s.getCreatedAt(), s.isArchived()));
    }

    @CheckOwnership(expression = "#sessionId", entityType = "SESSION")
    public Mono<Void> deleteSession(UUID sessionId, UUID userId) {
        return messageRepository.deleteAllBySessionId(sessionId)
                .then(Mono.fromRunnable(() -> chatMemory.clear(userId.toString() + ":" + sessionId.toString())))
                .then(sessionRepository.deleteById(sessionId));
    }

    @CheckOwnership(expression = "#sessionId", entityType = "SESSION")
    public Mono<ChatSessionResponse> updateSessionName(UUID sessionId, UUID userId, String newName) {
        return sessionRepository.findById(sessionId)
                .flatMap(s -> {
                    s.setName(newName);
                    return sessionRepository.save(s);
                })
                .map(s -> new ChatSessionResponse(s.getId(), s.getName(), s.getCreatedAt(), s.isArchived()));
    }

    @CheckOwnership(expression = "#sessionId", entityType = "SESSION")
    public Mono<ChatSessionResponse> setSessionArchived(UUID sessionId, UUID userId, boolean archived) {
        return sessionRepository.findById(sessionId)
                .flatMap(s -> {
                    s.setArchived(archived);
                    return sessionRepository.save(s);
                })
                .map(s -> new ChatSessionResponse(s.getId(), s.getName(), s.getCreatedAt(), s.isArchived()));
    }

    public Mono<CursorPageResponse<ChatSessionResponse>> listArchivedSessions(UUID userId, String cursor, int size) {
        Flux<ChatSession> sessionFlux;
        if (cursor == null || cursor.isBlank()) {
            sessionFlux = sessionRepository.findArchivedSessions(userId, size + 1);
        } else {
            Instant cursorTime = Instant.parse(cursor);
            sessionFlux = sessionRepository.findArchivedSessionsWithCursor(userId, cursorTime, size + 1);
        }

        return sessionFlux.collectList().map(sessions -> {
            boolean hasNext = sessions.size() > size;
            List<ChatSession> pagedSessions = hasNext ? sessions.subList(0, size) : sessions;
            String nextCursor = pagedSessions.isEmpty() ? null : pagedSessions.get(pagedSessions.size() - 1).getCreatedAt().toString();
            
            List<ChatSessionResponse> responses = pagedSessions.stream()
                    .map(s -> new ChatSessionResponse(s.getId(), s.getName(), s.getCreatedAt(), s.isArchived()))
                    .toList();
            return new CursorPageResponse<>(responses, nextCursor, hasNext, size);
        });
    }

    public Mono<Void> deleteArchivedSessions(List<UUID> sessionIds, UUID userId) {
        // Here we might need a bulk ownership check if we wanted to be 100% strict
        return sessionRepository.deleteAllById(sessionIds);
    }
}
