package com.bbmovie.ai_assistant_service.core.low_level._service._impl;

import com.bbmovie.ai_assistant_service.core.low_level._SessionNotFoundException;
import com.bbmovie.ai_assistant_service.core.low_level._dto._response._ChatSessionResponse;
import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatSession;
import com.bbmovie.ai_assistant_service.core.low_level._repository._ChatMessageRepository;
import com.bbmovie.ai_assistant_service.core.low_level._repository._ChatSessionRepository;
import com.bbmovie.ai_assistant_service.core.low_level._service._SessionService;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._Logger;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._LoggerFactory;
import com.bbmovie.common.dtos.CursorPageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class _SessionServiceImpl implements _SessionService {

    private static final _Logger log = _LoggerFactory.getLogger(_SessionServiceImpl.class);

    private final _ChatSessionRepository sessionRepository;
    private final _ChatMessageRepository messageRepository;

    @Autowired
    public _SessionServiceImpl(_ChatSessionRepository sessionRepository, _ChatMessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public Mono<CursorPageResponse<_ChatSessionResponse>> activeSessionsWithCursor(
            UUID userId, String cursor, int size) {

        Instant cursorTime = cursor != null
                ? Instant.parse(new String(Base64.getDecoder().decode(cursor)))
                : Instant.now();

        int limit = size + 1;

        return sessionRepository.findActiveSessionsWithCursor(userId, cursorTime, limit)
                .collectList()
                .map(sessions -> {
                    boolean hasMore = sessions.size() > size;

                    List<_ChatSession> content = hasMore
                            ? sessions.subList(0, size)
                            : sessions;

                    String nextCursor = null;
                    if (hasMore && !content.isEmpty()) {
                        _ChatSession last = content.getLast();
                        nextCursor = Base64.getEncoder()
                                .encodeToString(last.getUpdatedAt().toString().getBytes());
                    }

                    List<_ChatSessionResponse> contentResponse = content.stream()
                            .map(_ChatSessionResponse::fromEntity)
                            .toList();

                    return CursorPageResponse.<_ChatSessionResponse>builder()
                            .content(contentResponse)
                            .nextCursor(nextCursor)
                            .hasMore(hasMore)
                            .size(size)
                            .build();
                });
    }

    @Override
    @Transactional
    public Mono<_ChatSessionResponse> createSession(UUID userId, String sessionName) {
        _ChatSession session = _ChatSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sessionName(sessionName)
                .build()
                .asNew();
        return sessionRepository.save(session)
                .flatMap(savedSession -> {
                    _ChatSessionResponse response = _ChatSessionResponse.fromEntity(savedSession);
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
    public Mono<_ChatSession> getAndValidateSessionOwnership(UUID sessionId, UUID userId) {
        return sessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new _SessionNotFoundException("Session not found: " + sessionId)))
                .flatMap(session -> {
                    if (!session.getUserId().equals(userId)) {
                        log.warn("Access denied: User {} attempted to access session {} owned by {}",
                                userId, sessionId, session.getUserId());
                        return Mono.error(new SecurityException("User does not own session " + sessionId));
                    }
                    return Mono.just(session);
                });
    }

    @Transactional
    public Mono<_ChatSessionResponse> updateSessionName(UUID sessionId, UUID userId, String newName) {
        return getAndValidateSessionOwnership(sessionId, userId)
                .flatMap(session -> {
                    if (session.getSessionName().equals(newName)) {
                        return Mono.just(session); // No change, just return
                    }
                    session.setSessionName(newName);
                    return sessionRepository.save(session);
                })
                .map(_ChatSessionResponse::fromEntity);
    }

    @Transactional
    public Mono<_ChatSessionResponse> setSessionArchived(UUID sessionId, UUID userId, boolean isArchived) {
        return getAndValidateSessionOwnership(sessionId, userId)
                .flatMap(session -> {
                    if (session.isArchived() == isArchived) {
                        return Mono.just(session); // No change
                    }
                    session.setArchived(isArchived);
                    return sessionRepository.save(session);
                })
                .map(_ChatSessionResponse::fromEntity);
    }

    public Mono<CursorPageResponse<_ChatSessionResponse>> listArchivedSessions(
            UUID userId, String cursor, int size) {

        Instant cursorTime = cursor != null
                ? Instant.parse(new String(Base64.getDecoder().decode(cursor)))
                : Instant.now();

        int limit = size + 1;

        return  sessionRepository.findArchivedSessionsWithCursor(userId, cursorTime, limit)
                .collectList()
                .map(sessions -> {
                    boolean hasMore = sessions.size() > size;
                    List<_ChatSession> content = hasMore
                            ? sessions.subList(0, size)
                            : sessions;

                    String nextCursor = null;
                    if (hasMore && !content.isEmpty()) {
                        _ChatSession last = content.getLast();
                        nextCursor = Base64.getEncoder()
                                .encodeToString(last.getUpdatedAt().toString().getBytes());
                    }

                    List<_ChatSessionResponse> contentResponse = content.stream()
                            .map(_ChatSessionResponse::fromEntity)
                            .toList();

                    return CursorPageResponse.<_ChatSessionResponse>builder()
                            .content(contentResponse)
                            .nextCursor(nextCursor)
                            .hasMore(hasMore)
                            .size(size)
                            .build();
                });
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
                .filter(_ChatSession::isArchived)
                .flatMap(session ->
                        messageRepository.deleteBySessionId(session.getId())
                                .then(sessionRepository.delete(session))
                )
                .then(); // Convert to Mono<Void>
    }
}