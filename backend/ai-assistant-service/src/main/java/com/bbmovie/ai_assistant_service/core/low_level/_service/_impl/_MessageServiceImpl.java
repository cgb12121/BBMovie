package com.bbmovie.ai_assistant_service.core.low_level._service._impl;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatMessage;
import com.bbmovie.ai_assistant_service.core.low_level._entity._Sender;
import com.bbmovie.ai_assistant_service.core.low_level._repository._ChatMessageRepository;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._SessionService;
import com.example.common.dtos.CursorPageResponse;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class _MessageServiceImpl implements _MessageService {

    private final R2dbcEntityOperations r2dbcOperations;
    private final _ChatMessageRepository repository;
    private final _SessionService sessionService; // For ownership validation

    @Autowired
    public _MessageServiceImpl(
            @Qualifier("_EntityOperations") R2dbcEntityOperations r2dbcOperations,
            _ChatMessageRepository repository, _SessionService sessionService) {
        this.r2dbcOperations = r2dbcOperations;
        this.repository = repository;
        this.sessionService = sessionService;
    }

    @Override
    public Mono<_ChatMessage> saveUserMessage(UUID sessionId, String message) {
        return createAndSaveMessage(sessionId, _Sender.USER, message);
    }

    @Override
    public Mono<_ChatMessage> saveAiResponse(UUID sessionId, String content) {
        return createAndSaveMessage(sessionId, _Sender.AI, content);
    }

    private Mono<_ChatMessage> createAndSaveMessage(UUID sessionId, _Sender sender, String content) {
        _ChatMessage message = _ChatMessage.builder()
                .sessionId(sessionId)
                .sender(sender)
                .content(content)
                .build();
        return repository.save(message);
    }

    @Override
    public Mono<CursorPageResponse<_ChatMessage>> getMessagesWithCursor(
            UUID sessionId, UUID userId, String cursor, int size) {

        return sessionService.getAndValidateSessionOwnership(sessionId, userId)
                .flatMap(session -> {
                    Instant cursorTime = cursor != null
                            ? Instant.parse(new String(Base64.getDecoder().decode(cursor)))
                            : Instant.now();

                    Criteria criteria = Criteria.where("session_id").is(sessionId)
                            .and("timestamp").lessThan(cursorTime);

                    Sort sort = Sort.by(Sort.Order.desc("timestamp"));
                    Query query = Query.query(criteria)
                            .sort(sort)
                            .limit(size + 1);

                    return r2dbcOperations
                            .select(_ChatMessage.class)
                            .matching(query)
                            .all()
                            .collectList()
                            .map(messages -> {
                                boolean hasMore = messages.size() > size;
                                List<_ChatMessage> content = hasMore
                                        ? messages.subList(0, size)
                                        : messages;

                                String nextCursor = null;
                                if (hasMore && !content.isEmpty()) {
                                    _ChatMessage last = content.getLast();
                                    nextCursor = Base64.getEncoder()
                                            .encodeToString(last.getTimestamp().toString().getBytes());
                                }

                                // Reverse to get the oldest -> the newest order for display
                                Collections.reverse(content);

                                return CursorPageResponse.<_ChatMessage>builder()
                                        .content(content)
                                        .nextCursor(nextCursor)
                                        .hasMore(hasMore)
                                        .size(size)
                                        .build();
                            });
                });
    }
}
