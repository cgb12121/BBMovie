package com.bbmovie.ai_assistant_service.core.low_level._controller;

import com.bbmovie.ai_assistant_service.core.low_level._dto.ChatRequestDto;
import com.bbmovie.ai_assistant_service.core.low_level._dto.CreateSessionDto;
import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatSession;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model.AssistantType;
import com.bbmovie.ai_assistant_service.core.low_level._service._ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.example.common.entity.JoseConstraint.JosePayload.SUB;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class _ChatController {

    private final _ChatService chatService;

    @GetMapping("/sessions")
    public Flux<_ChatSession> getSessions(@AuthenticationPrincipal Jwt jwt) {
        return chatService.listSessionsForUser(jwt.getClaim(SUB));
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<_ChatSession> createSession(@RequestBody CreateSessionDto dto, @AuthenticationPrincipal Jwt jwt) {
        return chatService.createSession(jwt.getClaim(SUB), dto.getSessionName());
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteSession(@PathVariable UUID sessionId, @AuthenticationPrincipal Jwt jwt) {
        return chatService.deleteSession(sessionId, jwt.getClaim(SUB));
    }

    @PostMapping(value = "/chat/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(
            @PathVariable UUID sessionId,
            @RequestBody ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {

        AssistantType assistantType = AssistantType.fromCode(request.getAssistantType());

        return chatService.chat(sessionId, request.getMessage(), assistantType, jwt)
                .map(chunk -> ServerSentEvent.builder(chunk).build())
                .doOnError(ex -> log.error("Streaming error for session {}: {}", sessionId, ex.getMessage()));
    }
}
