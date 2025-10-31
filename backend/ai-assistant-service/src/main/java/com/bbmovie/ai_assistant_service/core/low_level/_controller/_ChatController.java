package com.bbmovie.ai_assistant_service.core.low_level._controller;

import com.bbmovie.ai_assistant_service.core.low_level._dto.ChatRequestDto;
import com.bbmovie.ai_assistant_service.core.low_level._dto.CreateSessionDto;
import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatSession;
import com.bbmovie.ai_assistant_service.core.low_level._model.AssistantType;
import com.bbmovie.ai_assistant_service.core.low_level._service._ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class _ChatController {

    private final _ChatService chatService;

    @GetMapping("/sessions")
    public Flux<_ChatSession> getSessions(Principal principal) {
        return chatService.listSessionsForUser(principal.getName());
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<_ChatSession> createSession(@RequestBody CreateSessionDto dto, Principal principal) {
        return chatService.createSession(principal.getName(), dto.getSessionName());
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteSession(@PathVariable String sessionId, Principal principal) {
        return chatService.deleteSession(sessionId, principal.getName());
    }

    @PostMapping(value = "/chat/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(
            @PathVariable String sessionId,
            @RequestBody ChatRequestDto request,
            Principal principal) {

        AssistantType assistantType = AssistantType.fromCode(request.getAssistantType());

        return chatService.chat(sessionId, request.getMessage(), assistantType, principal)
                .map(chunk -> ServerSentEvent.builder(chunk).build())
                .doOnError(ex -> log.error("Streaming error for session {}: {}", sessionId, ex.getMessage()));
    }
}
