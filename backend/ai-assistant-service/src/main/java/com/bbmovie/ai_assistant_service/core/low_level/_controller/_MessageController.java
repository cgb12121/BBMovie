package com.bbmovie.ai_assistant_service.core.low_level._controller;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatMessage;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.example.common.dtos.ApiResponse;
import com.example.common.dtos.CursorPageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.example.common.entity.JoseConstraint.JosePayload.SUB;

@RestController
@RequestMapping("/api/v1/messages")
public class _MessageController {

    private final _MessageService messageService;

    @Autowired
    public _MessageController(_MessageService messageService) {
        this.messageService = messageService;
    }

    // Does not support deleting messages or "try again..." yet
    @GetMapping("/{sessionId}")
    public Mono<ApiResponse<CursorPageResponse<_ChatMessage>>> getMessages(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return messageService.getMessagesWithCursor(sessionId, userId, cursor, size)
                .map(ApiResponse::success);
    }
}
