package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatMessage;
import com.bbmovie.ai_assistant_service.core.low_level._entity._Sender;
import com.bbmovie.ai_assistant_service.core.low_level._repository._ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class _ChatMessageService {

    private final _ChatMessageRepository repository;

    public Mono<_ChatMessage> saveUserMessage(String sessionId, String message) {
        return createAndSaveMessage(sessionId, _Sender.USER, message);
    }

    public Mono<_ChatMessage> saveAiResponse(String sessionId, String content) {
        return createAndSaveMessage(sessionId, _Sender.AI, content);
    }

    public Mono<_ChatMessage> saveToolRequest(String sessionId, String content) {
        return createAndSaveMessage(sessionId, _Sender.TOOL_REQUEST, content);
    }

    public Mono<_ChatMessage> saveToolResult(String sessionId, String content) {
        return createAndSaveMessage(sessionId, _Sender.TOOL_RESULT, content);
    }

    private Mono<_ChatMessage> createAndSaveMessage(String sessionId, _Sender sender, String content) {
        _ChatMessage message = _ChatMessage.builder()
                .sessionId(sessionId)
                .sender(sender)
                .content(content)
                .timestamp(Instant.now())
                .build();
        return repository.save(message);
    }
}