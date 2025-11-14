package com.bbmovie.ai_assistant_service.core.low_level._service._impl;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatMessage;
import com.bbmovie.ai_assistant_service.core.low_level._entity._Sender;
import com.bbmovie.ai_assistant_service.core.low_level._repository._ChatMessageRepository;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._SessionService;
import com.bbmovie.common.dtos.CursorPageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class _MessageServiceImpl implements _MessageService {

    private final _ChatMessageRepository repository;
    private final _SessionService sessionService; // For ownership validation

    @Autowired
    public _MessageServiceImpl(_ChatMessageRepository repository, _SessionService sessionService) {
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
                            ? Instant.parse(new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8))
                            : Instant.now();

                    // We fetch one more than requested to see if there 'hasMore'
                    int querySize = size + 1;

                    return repository.getWithCursor(sessionId, cursorTime, querySize)
                            .collectList()
                            .map(messages -> {
                                boolean hasMore = messages.size() > size;
                                List<_ChatMessage> content = hasMore
                                        ? messages.subList(0, size)
                                        : messages;

                                String nextCursor = null;
                                if (hasMore && !content.isEmpty()) {
                                    // Get the last item in the *returned* list
                                    _ChatMessage last = content.getLast();
                                    nextCursor = Base64.getEncoder()
                                            .encodeToString(last.getTimestamp().toString().getBytes(StandardCharsets.UTF_8));
                                }

                                // Reverse to get the oldest -> the newest order for display
                                Collections.reverse(content);

                                return CursorPageResponse.<_ChatMessage>builder()
                                        .content(content)
                                        .nextCursor(nextCursor)
                                        .hasMore(hasMore)
                                        .size(content.size()) // Return the actual size of the content list
                                        .build();
                            });
                });
    }
}
