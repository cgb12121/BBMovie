package com.bbmovie.ai_assistant_service.core.low_level._controller;

import com.bbmovie.ai_assistant_service.core.low_level._dto._request._ChatRequestDto;
import com.bbmovie.ai_assistant_service.core.low_level._dto._response._ChatStreamChunk;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AssistantType;
import com.bbmovie.ai_assistant_service.core.low_level._service._ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
public class _ChatController {

    private final _ChatService chatService;

    @Autowired
    public _ChatController(_ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<_ChatStreamChunk>> streamChat(
            @PathVariable UUID sessionId,
            @RequestBody _ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        _AssistantType assistantType = _AssistantType.fromCode(request.getAssistantType());
        return chatService.chat(sessionId, request.getMessage(), assistantType, jwt)
                .map(chunk -> ServerSentEvent.builder(chunk).build())
                .doOnError(ex -> log.error("Streaming error for session {}: {}", sessionId, ex.getMessage()));
    }

    // blocked stream for better postman testing
    @PostMapping("/{sessionId}/test")
    public Mono<Map<String, Object>> nonStreamChat(
            @PathVariable UUID sessionId,
            @RequestBody _ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        _AssistantType assistantType = _AssistantType.fromCode(request.getAssistantType());

        return chatService.chat(sessionId, request.getMessage(), assistantType, jwt)
                .collectList()
                .flatMap(chunks -> {
                    String combinedContent = chunks.stream()
                            .filter(chunk -> "assistant".equals(chunk.getType()))
                            .map(_ChatStreamChunk::getContent)
                            .collect(Collectors.joining());
                    Map<String, Object> metadata = chunks.stream()
                            .filter(chunk -> "assistant".equals(chunk.getType()))
                            .map(_ChatStreamChunk::getMetadata)
                            .flatMap(m -> m.entrySet().stream())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (a, b) -> b)
                            );

                    Map<String, Object> response = Map.of(
                            "type", "assistant",
                            "content", combinedContent,
                            "metadata", metadata
                    );

                    return Mono.just(response);
                });
    }

}
