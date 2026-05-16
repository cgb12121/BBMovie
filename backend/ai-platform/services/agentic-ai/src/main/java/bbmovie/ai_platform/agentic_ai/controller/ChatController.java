package bbmovie.ai_platform.agentic_ai.controller;

import bbmovie.ai_platform.agentic_ai.dto.request.ChatRequestDto;
import bbmovie.ai_platform.agentic_ai.dto.request.EditMessageRequest;
import bbmovie.ai_platform.agentic_ai.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;
import java.util.UUID;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ROLE;

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
            @Valid @RequestBody ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String userRole = jwt.getClaimAsString(ROLE);
        
        return chatService.chat(
                sessionId, userId, request.message(), request.parentId(), 
                request.assetId(), request.aiMode(), request.model(), userRole
            );
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
            @Valid @RequestBody EditMessageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return chatService.editMessage(messageId, userId, request.newContent());
    }
}
