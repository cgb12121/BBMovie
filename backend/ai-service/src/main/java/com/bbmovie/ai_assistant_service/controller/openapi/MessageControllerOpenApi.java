package com.bbmovie.ai_assistant_service.controller.openapi;

import com.bbmovie.ai_assistant_service.dto.response.ChatMessageResponse;
import com.bbmovie.common.dtos.ApiResponse;
import com.bbmovie.common.dtos.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.UUID;

@SuppressWarnings("unused")
@Tag(name = "AI Messages", description = "Chat message retrieval APIs")
public interface MessageControllerOpenApi {
    @Operation(
            summary = "Get session messages",
            description = "Get messages for one session with cursor pagination",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    Mono<ApiResponse<CursorPageResponse<ChatMessageResponse>>> getMessages(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt
    );
}

