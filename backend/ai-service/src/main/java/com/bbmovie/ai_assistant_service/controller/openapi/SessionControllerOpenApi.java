package com.bbmovie.ai_assistant_service.controller.openapi;

import com.bbmovie.ai_assistant_service.dto.request.CreateSessionDto;
import com.bbmovie.ai_assistant_service.dto.request.DeleteArchivedSessionsDto;
import com.bbmovie.ai_assistant_service.dto.request.SetSessionArchivedDto;
import com.bbmovie.ai_assistant_service.dto.request.UpdateSessionNameDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatSessionResponse;
import com.bbmovie.common.dtos.ApiResponse;
import com.bbmovie.common.dtos.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Mono;

import java.util.UUID;

@SuppressWarnings("unused")
@Tag(name = "AI Sessions", description = "AI chat session management APIs")
public interface SessionControllerOpenApi {
    @Operation(summary = "Get sessions", description = "List active sessions with cursor pagination", security = @SecurityRequirement(name = "bearerAuth"))
    Mono<ApiResponse<CursorPageResponse<ChatSessionResponse>>> getSessions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal Jwt jwt
    );

    @Operation(summary = "Create session", description = "Create a new chat session", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Session created"))
    @ResponseStatus(HttpStatus.CREATED)
    Mono<ApiResponse<ChatSessionResponse>> createSession(
            @RequestBody @Valid CreateSessionDto dto,
            @AuthenticationPrincipal Jwt jwt
    );

    @Operation(summary = "Delete session", description = "Delete one session by ID", security = @SecurityRequirement(name = "bearerAuth"))
    Mono<ApiResponse<String>> deleteSession(@PathVariable UUID sessionId, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Update session name", description = "Rename a session", security = @SecurityRequirement(name = "bearerAuth"))
    Mono<ApiResponse<ChatSessionResponse>> updateSessionName(
            @PathVariable UUID sessionId,
            @RequestBody @Valid UpdateSessionNameDto dto,
            @AuthenticationPrincipal Jwt jwt
    );

    @Operation(summary = "Archive/unarchive session", description = "Set archive status for one session", security = @SecurityRequirement(name = "bearerAuth"))
    Mono<ApiResponse<ChatSessionResponse>> setSessionArchived(
            @PathVariable UUID sessionId,
            @RequestBody @Valid SetSessionArchivedDto dto,
            @AuthenticationPrincipal Jwt jwt
    );

    @Operation(summary = "List archived sessions", description = "List archived sessions with cursor pagination", security = @SecurityRequirement(name = "bearerAuth"))
    Mono<ApiResponse<CursorPageResponse<ChatSessionResponse>>> listArchivedSessions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal Jwt jwt
    );

    @Operation(summary = "Delete archived sessions", description = "Bulk delete archived sessions", security = @SecurityRequirement(name = "bearerAuth"))
    Mono<ApiResponse<String>> deleteArchivedSessions(
            @RequestBody @Valid DeleteArchivedSessionsDto dto,
            @AuthenticationPrincipal Jwt jwt
    );
}

