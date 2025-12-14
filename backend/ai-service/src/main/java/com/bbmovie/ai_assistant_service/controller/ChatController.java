package com.bbmovie.ai_assistant_service.controller;

import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;

import java.util.UUID;

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
     * @param request The request body containing message, AI mode, and optional attachments
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
}
