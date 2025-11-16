package com.bbmovie.ai_assistant_service.service.impl;

import com.bbmovie.ai_assistant_service.entity.ChatMessage;
import com.bbmovie.ai_assistant_service.entity.Sender;
import com.bbmovie.ai_assistant_service.repository.ChatMessageRepository;
import com.bbmovie.ai_assistant_service.service.MessageService;
import com.bbmovie.ai_assistant_service.service.SessionService;
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
public class MessageServiceImpl implements MessageService {

    private final ChatMessageRepository repository;
    private final SessionService sessionService; // For ownership validation

    @Autowired
    public MessageServiceImpl(ChatMessageRepository repository, SessionService sessionService) {
        this.repository = repository;
        this.sessionService = sessionService;
    }

    @Override
    public Mono<ChatMessage> saveUserMessage(UUID sessionId, String message) {
        return createAndSaveMessage(sessionId, Sender.USER, message);
    }

    @Override
    public Mono<ChatMessage> saveAiResponse(UUID sessionId, String content) {
        return createAndSaveMessage(sessionId, Sender.AI, content);
    }

    private Mono<ChatMessage> createAndSaveMessage(UUID sessionId, Sender sender, String content) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .sender(sender)
                .content(content)
                .build();
        return repository.save(message);
    }

    @Override
    public Mono<CursorPageResponse<ChatMessage>> getMessagesWithCursor(
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
                                List<ChatMessage> content = hasMore
                                        ? messages.subList(0, size)
                                        : messages;

                                String nextCursor = null;
                                if (hasMore && !content.isEmpty()) {
                                    // Get the last item in the *returned* list
                                    ChatMessage last = content.getLast();
                                    nextCursor = Base64.getEncoder()
                                            .encodeToString(last.getTimestamp().toString().getBytes(StandardCharsets.UTF_8));
                                }

                                // Reverse to get the oldest -> the newest order for display
                                Collections.reverse(content);

                                return CursorPageResponse.<ChatMessage>builder()
                                        .content(content)
                                        .nextCursor(nextCursor)
                                        .hasMore(hasMore)
                                        .size(content.size()) // Return the actual size of the content list
                                        .build();
                            });
                });
    }
}
