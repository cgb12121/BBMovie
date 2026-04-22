package com.bbmovie.ai_assistant_service.controller.openapi;

import com.bbmovie.ai_assistant_service.dto.request.ApprovalDecisionDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Tag(name = "AI Approval", description = "Approval workflow endpoints for AI actions")
public interface ApprovalControllerOpenApi {
    @Operation(
            summary = "Handle approval decision",
            description = "Submits approval/rejection decision and streams AI continuation",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Decision processed",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(implementation = ChatStreamChunk.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    Flux<ServerSentEvent<ChatStreamChunk>> handleApprovalDecision(
            @PathVariable UUID sessionId,
            @PathVariable String requestId,
            @RequestBody ApprovalDecisionDto decisionBody,
            @AuthenticationPrincipal Jwt jwt
    );
}

