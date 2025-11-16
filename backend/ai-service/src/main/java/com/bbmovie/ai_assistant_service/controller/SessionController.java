package com.bbmovie.ai_assistant_service.controller;

import com.bbmovie.ai_assistant_service.dto.request.CreateSessionDto;
import com.bbmovie.ai_assistant_service.dto.request.DeleteArchivedSessionsDto;
import com.bbmovie.ai_assistant_service.dto.request.SetSessionArchivedDto;
import com.bbmovie.ai_assistant_service.dto.request.UpdateSessionNameDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatSessionResponse;
import com.bbmovie.ai_assistant_service.service.SessionService;
import com.bbmovie.common.dtos.ApiResponse;
import com.bbmovie.common.dtos.CursorPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.SUB;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public Mono<ApiResponse<CursorPageResponse<ChatSessionResponse>>> getSessions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.activeSessionsWithCursor(userId, cursor, size)
                .map(ApiResponse::success);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<ChatSessionResponse>> createSession(
            @RequestBody @Valid CreateSessionDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.createSession(userId, dto.getSessionName())
                .map(ApiResponse::success);
    }

    @DeleteMapping("/{sessionId}")
    public Mono<ApiResponse<String>> deleteSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.deleteSession(sessionId, userId)
                .then(Mono.fromCallable(() -> ApiResponse.success("Session deleted successfully")));
    }

    @PatchMapping("/{sessionId}/name")
    public Mono<ApiResponse<ChatSessionResponse>> updateSessionName(
            @PathVariable UUID sessionId,
            @RequestBody @Valid UpdateSessionNameDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.updateSessionName(sessionId, userId, dto.getNewName())
                .map(ApiResponse::success);
    }

    @PatchMapping("/{sessionId}/archived")
    public Mono<ApiResponse<ChatSessionResponse>> setSessionArchived(
            @PathVariable UUID sessionId,
            @RequestBody @Valid SetSessionArchivedDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.setSessionArchived(sessionId, userId, dto.isArchived())
                .map(ApiResponse::success);
    }

    @GetMapping("/archived")
    public Mono<ApiResponse<CursorPageResponse<ChatSessionResponse>>> listArchivedSessions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.listArchivedSessions(userId, cursor, size)
                .map(ApiResponse::success);
    }

    @DeleteMapping("/archived")
    public Mono<ApiResponse<String>> deleteArchivedSessions(
            @RequestBody @Valid DeleteArchivedSessionsDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.deleteArchivedSessions(dto.getSessionIds(), userId)
                .then(Mono.fromCallable(() -> ApiResponse.success("Archived sessions deleted successfully")));
    }
}
