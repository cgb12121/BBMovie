package bbmovie.ai_platform.agentic_ai.controller;

import bbmovie.ai_platform.agentic_ai.dto.request.ChatRequestDto;
import bbmovie.ai_platform.agentic_ai.dto.request.EditMessageRequest;
import bbmovie.ai_platform.agentic_ai.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping(
        value = "/{sessionId}", 
        consumes = MediaType.APPLICATION_JSON_VALUE, 
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> streamChat(
            @PathVariable UUID sessionId,
            @RequestBody ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return chatService.chat(sessionId, userId, request.message(), request.parentId(), request.aiMode(), request.model());
    }

    @PostMapping(
        value = "/{messageId}/regenerate", 
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> regenerateMessage(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return chatService.regenerateMessage(messageId, userId);
    }

    @PutMapping(
        value = "/{messageId}", 
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> editMessage(
            @PathVariable UUID messageId,
            @RequestBody EditMessageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return chatService.editMessage(messageId, userId, request.newContent());
    }
}
