package com.bbmovie.ai_assistant_service.core.low_level._controller;

import com.bbmovie.ai_assistant_service.core.low_level._dto._ChatRequestDto;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model.AssistantType;
import com.bbmovie.ai_assistant_service.core.low_level._service._ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

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
    public Flux<ServerSentEvent<String>> streamChat(
            @PathVariable UUID sessionId,
            @RequestBody _ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        AssistantType assistantType = AssistantType.fromCode(request.getAssistantType());
        return chatService.chat(sessionId, request.getMessage(), assistantType, jwt)
                .map(chunk -> ServerSentEvent.builder(chunk).build())
                .doOnError(ex -> log.error("Streaming error for session {}: {}", sessionId, ex.getMessage()));
    }

    @PostMapping("/v0/{sessionId}/")
    public Flux<String> nonStreamChat(
            @PathVariable UUID sessionId,
            @RequestBody _ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        AssistantType assistantType = AssistantType.fromCode(request.getAssistantType());
        return chatService.chat(sessionId, request.getMessage(), assistantType, jwt);
    }
}
