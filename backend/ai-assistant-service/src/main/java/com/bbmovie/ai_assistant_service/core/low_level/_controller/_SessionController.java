package com.bbmovie.ai_assistant_service.core.low_level._controller;

import com.bbmovie.ai_assistant_service.core.low_level._dto._request._CreateSessionDto;
import com.bbmovie.ai_assistant_service.core.low_level._dto._request._DeleteArchivedSessionsDto;
import com.bbmovie.ai_assistant_service.core.low_level._dto._request._SetSessionArchivedDto;
import com.bbmovie.ai_assistant_service.core.low_level._dto._request._UpdateSessionNameDto;
import com.bbmovie.ai_assistant_service.core.low_level._dto._response._ChatSessionResponse;
import com.bbmovie.ai_assistant_service.core.low_level._service._SessionService;
import com.example.common.dtos.ApiResponse;
import com.example.common.dtos.CursorPageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.example.common.entity.JoseConstraint.JosePayload.SUB;

@RestController
@RequestMapping("/api/v1/sessions")
public class _SessionController {

    private final _SessionService sessionService;

    @Autowired
    public _SessionController(_SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public Mono<ApiResponse<CursorPageResponse<_ChatSessionResponse>>> getSessions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.activeSessionsWithCursor(userId, cursor, size)
                .map(ApiResponse::success);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<_ChatSessionResponse>> createSession(
            @RequestBody _CreateSessionDto dto, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.createSession(userId, dto.getSessionName())
                .map(ApiResponse::success);
    }

    @DeleteMapping("/{sessionId}")
    public Mono<ApiResponse<String>> deleteSession(@PathVariable UUID sessionId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.deleteSession(sessionId, userId)
                .then(Mono.fromCallable(() -> ApiResponse.success("Session deleted successfully")));
    }

    @PatchMapping("/{sessionId}/name")
    public Mono<ApiResponse<_ChatSessionResponse>> updateSessionName(
            @PathVariable UUID sessionId,
            @RequestBody _UpdateSessionNameDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.updateSessionName(sessionId, userId, dto.getNewName())
                .map(ApiResponse::success);
    }

    @PatchMapping("/{sessionId}/archived")
    public Mono<ApiResponse<_ChatSessionResponse>> setSessionArchived(
            @PathVariable UUID sessionId,
            @RequestBody _SetSessionArchivedDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.setSessionArchived(sessionId, userId, dto.isArchived())
                .map(ApiResponse::success);
    }

    @GetMapping("/archived")
    public Mono<ApiResponse<CursorPageResponse<_ChatSessionResponse>>> listArchivedSessions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.listArchivedSessions(userId, cursor, size)
                .map(ApiResponse::success);
    }

    @DeleteMapping("/archived")
    public Mono<ApiResponse<String>> deleteArchivedSessions(
            @RequestBody _DeleteArchivedSessionsDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.deleteArchivedSessions(dto.getSessionIds(), userId)
                .then(Mono.fromCallable(() -> ApiResponse.success("Archived sessions deleted successfully")));
    }

}
