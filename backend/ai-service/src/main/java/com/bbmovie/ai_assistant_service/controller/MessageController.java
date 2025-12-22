package com.bbmovie.ai_assistant_service.controller;

import com.bbmovie.ai_assistant_service.dto.response.ChatMessageResponse;
import com.bbmovie.ai_assistant_service.service.MessageService;
import com.bbmovie.common.dtos.ApiResponse;
import com.bbmovie.common.dtos.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.SUB;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    // Does not support deleting sent messages or "try again..." yet
    @GetMapping("/{sessionId}")
    public Mono<ApiResponse<CursorPageResponse<ChatMessageResponse>>> getMessages(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return messageService.getMessagesWithCursor(sessionId, userId, cursor, size)
                .map(ApiResponse::success);
    }
}
