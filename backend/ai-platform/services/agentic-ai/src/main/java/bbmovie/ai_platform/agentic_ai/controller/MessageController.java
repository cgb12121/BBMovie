package bbmovie.ai_platform.agentic_ai.controller;

import bbmovie.ai_platform.agentic_ai.dto.request.EditMessageRequest;
import bbmovie.ai_platform.agentic_ai.dto.response.ChatMessageResponse;
import bbmovie.ai_platform.agentic_ai.service.MessageService;
import bbmovie.ai_platform.agentic_ai.service.ChatService;
import lombok.RequiredArgsConstructor;

import com.bbmovie.common.dtos.ApiResponse;
import com.bbmovie.common.dtos.CursorPageResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;
    private final ChatService chatService;

    @GetMapping("/{sessionId}")
    public Mono<ApiResponse<CursorPageResponse<ChatMessageResponse>>> getMessages(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return messageService.getMessagesWithCursor(sessionId, userId, cursor, size)
                .map(ApiResponse::success);
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
