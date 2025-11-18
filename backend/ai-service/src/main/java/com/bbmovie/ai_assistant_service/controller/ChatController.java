package com.bbmovie.ai_assistant_service.controller;

import  com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.service.ChatService;
import com.bbmovie.ai_assistant_service.utils.log.Logger;
import com.bbmovie.ai_assistant_service.utils.log.LoggerFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    @PostMapping(value = "/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamChunk>> streamChat(
            @PathVariable UUID sessionId,
            @RequestBody @Valid ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        return chatService.chat(sessionId, request, jwt)
                .map(chunk -> ServerSentEvent.builder(chunk).build())
                .doOnError(ex -> log.error("Streaming error for session {}: {}", sessionId, ex.getMessage()));
    }

    // blocked stream for better postman view testing
    @PostMapping("/{sessionId}/test")
    public Mono<Map<String, Object>> nonStreamChat(
            @PathVariable UUID sessionId,
            @RequestBody @Valid ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        return chatService.chat(sessionId,request, jwt)
                .collectList()
                .flatMap(chunks -> returnDebugResponse(sessionId, chunks));
    }

    private static Mono<Map<String, Object>> returnDebugResponse(UUID sessionId, List<ChatStreamChunk> chunks) {
        String combinedContent = chunks.stream()
                .filter(chunk -> "assistant".equals(chunk.getType()))
                .map(ChatStreamChunk::getContent)
                .collect(Collectors.joining());
        Map<String, Object> metadata = chunks.stream()
                .filter(chunk -> "assistant".equals(chunk.getType()))
                .map(ChatStreamChunk::getMetadata)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> b)
                );

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId.toString());
        response.put("type", "assistant");
        response.put("content", combinedContent);
        if (!metadata.isEmpty()) {
            response.put("metadata", metadata);
        }

        return Mono.just(response);
    }
}
