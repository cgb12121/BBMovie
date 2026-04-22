package com.bbmovie.ai_assistant_service.controller.openapi;

import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Tag(name = "AI Chat", description = "AI chat streaming endpoints")
public interface ChatControllerOpenApi {
    @Operation(
            summary = "Stream chat response",
            description = "Streams AI response chunks for a chat session",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stream opened",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(implementation = ChatStreamChunk.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    Flux<ServerSentEvent<ChatStreamChunk>> streamChat(
            @PathVariable UUID sessionId,
            @RequestBody @Valid ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt
    );
}

