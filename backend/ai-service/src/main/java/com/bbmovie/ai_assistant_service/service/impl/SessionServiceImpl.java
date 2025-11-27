package com.bbmovie.ai_assistant_service.service.impl;

import com.bbmovie.ai_assistant_service.exception.SecurityViolationException;
import com.bbmovie.ai_assistant_service.exception.SessionNotFoundException;
import com.bbmovie.ai_assistant_service.dto.response.ChatSessionResponse;
import com.bbmovie.ai_assistant_service.entity.ChatSession;
import com.bbmovie.ai_assistant_service.repository.ChatMessageRepository;
import com.bbmovie.ai_assistant_service.repository.ChatSessionRepository;
import com.bbmovie.ai_assistant_service.service.SessionService;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import com.bbmovie.common.dtos.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(SessionServiceImpl.class);

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    @Override
    public Mono<CursorPageResponse<ChatSessionResponse>> activeSessionsWithCursor(
            UUID userId, String cursor, int size) {

        Instant cursorTime = cursor != null
                ? Instant.parse(new String(Base64.getDecoder().decode(cursor)))
                : Instant.now();

        int limit = size + 1;

        return sessionRepository.findActiveSessionsWithCursor(userId, cursorTime, limit)
                .collectList()
                .map(sessions -> getCursorPageResponse(size, sessions));
    }

    @Override
    @Transactional
    public Mono<ChatSessionResponse> createSession(UUID userId, String sessionName) {
        ChatSession session = ChatSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sessionName(sessionName)
                .build()
                .asNew();
        return sessionRepository.save(session)
                .flatMap(savedSession -> {
                    ChatSessionResponse response = ChatSessionResponse.fromEntity(savedSession);
                    return Mono.just(response);
                });
    }

    @Override
    @Transactional
    public Mono<Void> deleteSession(UUID sessionId, UUID userId) {
        return getAndValidateSessionOwnership(sessionId, userId)
                .flatMap(session -> {
                    UUID id = session.getId();

                    if (session.isArchived()) {
                        return Mono.error(new IllegalArgumentException("Session is archived"));
                    }

                    return messageRepository.deleteBySessionId(id)
                            .then(sessionRepository.deleteById(id));
                });
    }

    @Override
    public Mono<ChatSession> getAndValidateSessionOwnership(UUID sessionId, UUID userId) {
        return sessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SessionNotFoundException("Session not found: " + sessionId)))
                .flatMap(session -> {
                    if (!session.getUserId().equals(userId)) {
                        log.warn("Access denied: User {} attempted to access session {} owned by {}",
                                userId, sessionId, session.getUserId());
                        return Mono.error(new SecurityViolationException("User does not own session " + sessionId));
                    }
                    return Mono.just(session);
                });
    }

    @Transactional
    public Mono<ChatSessionResponse> updateSessionName(UUID sessionId, UUID userId, String newName) {
        return getAndValidateSessionOwnership(sessionId, userId)
                .flatMap(session -> {
                    if (session.getSessionName().equals(newName)) {
                        return Mono.just(session); // No change, just return
                    }
                    session.setSessionName(newName);
                    return sessionRepository.save(session);
                })
                .map(ChatSessionResponse::fromEntity);
    }

    @Transactional
    public Mono<ChatSessionResponse> setSessionArchived(UUID sessionId, UUID userId, boolean isArchived) {
        return getAndValidateSessionOwnership(sessionId, userId)
                .flatMap(session -> {
                    if (session.isArchived() == isArchived) {
                        return Mono.just(session); // No change
                    }
                    session.setArchived(isArchived);
                    return sessionRepository.save(session);
                })
                .map(ChatSessionResponse::fromEntity);
    }

    public Mono<CursorPageResponse<ChatSessionResponse>> listArchivedSessions(
            UUID userId, String cursor, int size) {

        Instant cursorTime = cursor != null
                ? Instant.parse(new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8))
                : Instant.now();

        int limit = size + 1;

        return  sessionRepository.findArchivedSessionsWithCursor(userId, cursorTime, limit)
                .collectList()
                .map(sessions -> getCursorPageResponse(size, sessions));
    }

    @Transactional
    public Mono<Void> deleteArchivedSessions(List<UUID> sessionIds, UUID userId) {
        return Flux.fromIterable(sessionIds)
                .flatMap(sessionId -> deleteOneArchived(sessionId, userId))
                .then(); // Wait for all operations to complete
    }

    private Mono<Void> deleteOneArchived(UUID sessionId, UUID userId) {
        return sessionRepository.findById(sessionId)
                .filter(session -> session.getUserId().equals(userId))
                .filter(ChatSession::isArchived)
                .flatMap(session ->
                        messageRepository.deleteBySessionId(session.getId())
                                .then(sessionRepository.delete(session))
                )
                .then(); // Convert to Mono<Void>
    }

    private static CursorPageResponse<ChatSessionResponse> getCursorPageResponse(int size, List<ChatSession> sessions) {
        boolean hasMore = sessions.size() > size;

        List<ChatSession> content = hasMore
                ? sessions.subList(0, size)
                : sessions;

        String nextCursor = null;
        if (hasMore && !content.isEmpty()) {
            ChatSession last = content.getLast();
            nextCursor = Base64.getEncoder()
                    .encodeToString(last.getUpdatedAt().toString().getBytes(StandardCharsets.UTF_8));
        }

        List<ChatSessionResponse> contentResponse = content.stream()
                .map(ChatSessionResponse::fromEntity)
                .toList();

        return CursorPageResponse.<ChatSessionResponse>builder()
                .content(contentResponse)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .size(size)
                .build();
    }
}