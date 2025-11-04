package com.bbmovie.ai_assistant_service.core.low_level._service._impl;

import com.bbmovie.ai_assistant_service.core.low_level._SessionNotFoundException;
import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatSession;
import com.bbmovie.ai_assistant_service.core.low_level._repository._ChatMessageRepository;
import com.bbmovie.ai_assistant_service.core.low_level._repository._ChatSessionRepository;
import com.bbmovie.ai_assistant_service.core.low_level._service._SessionService;
import com.example.common.dtos.CursorPageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class _SessionServiceImpl implements _SessionService {

    private final R2dbcEntityOperations r2dbcOperations;
    private final _ChatSessionRepository sessionRepository;
    private final _ChatMessageRepository messageRepository;

    @Autowired
    public _SessionServiceImpl(
            @Qualifier("_EntityOperations") R2dbcEntityOperations r2dbcOperations,
            _ChatSessionRepository sessionRepository, _ChatMessageRepository messageRepository) {
        this.r2dbcOperations = r2dbcOperations;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public Mono<CursorPageResponse<_ChatSession>> listSessionsWithCursor(
            UUID userId, String cursor, int size) {

        Instant cursorTime = cursor != null
                ? Instant.parse(new String(Base64.getDecoder().decode(cursor)))
                : Instant.now();

        Criteria criteria = Criteria.where("user_id").is(userId)
                .and("updated_at").lessThan(cursorTime);

        Sort sort = Sort.by(Sort.Order.desc("updated_at"));
        Query query = Query.query(criteria)
                .sort(sort)
                .limit(size + 1);  // Fetch one extra to check if more exist

        return r2dbcOperations
                .select(_ChatSession.class)
                .matching(query)
                .all()
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

                    return CursorPageResponse.<_ChatSession>builder()
                            .content(content)
                            .nextCursor(nextCursor)
                            .hasMore(hasMore)
                            .size(size)
                            .build();
                });
    }

    @Override
    public Mono<_ChatSession> createSession(UUID userId, String sessionName) {
        _ChatSession session = _ChatSession.builder()
                .userId(userId)
                .sessionName(sessionName)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return sessionRepository.save(session);
    }

    @Override
    public Mono<Void> deleteSession(UUID sessionId, UUID userId) {
        return getAndValidateSessionOwnership(sessionId, userId)
                .flatMap(session -> messageRepository.deleteBySessionId(session.getId())
                        .then(sessionRepository.deleteById(session.getId()))
                );
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
}
