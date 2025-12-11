package com.bbmovie.ai_assistant_service.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.service.ChatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controller for chat endpoints.
 * <p>
 * Endpoints:
 * - POST /{sessionId} - Unified JSON request for text and file attachments
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    /**
     * Unified Stream Chat Endpoint.
     * <p>
     * Handles both simple text messages and messages with file attachments.
     * The client must upload file attachments to the file-service first.
     * The client then passes the file metadata (id, url) in the ChatRequestDto.
     * 
     * @param sessionId Chat session ID
     * @param request Request body containing message, AI mode, and optional attachments
     * @param jwt User authentication token
     * @return Server-sent events stream of chat responses
     */
    @PostMapping(
            value = "/{sessionId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<ChatStreamChunk>> streamChat(
            @PathVariable UUID sessionId,
            @RequestBody @Valid ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        return chatService.chat(sessionId, request, jwt)
                .map(chunk -> ServerSentEvent.builder(chunk).build());
    }

    /**
     * Debug endpoint for non-streaming response (useful for Postman testing).
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{sessionId}/test")
    public Mono<Map<String, Object>> nonStreamChat(
            @PathVariable UUID sessionId,
            @RequestBody @Valid ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        return chatService.chat(sessionId, request, jwt)
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
                        (a, b) -> b));

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