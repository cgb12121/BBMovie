package bbmovie.ai_platform.agentic_ai.controller;

import bbmovie.ai_platform.agentic_ai.dto.request.CreateSessionDto;
import bbmovie.ai_platform.agentic_ai.dto.request.DeleteArchivedSessionsDto;
import bbmovie.ai_platform.agentic_ai.dto.request.SetSessionArchivedDto;
import bbmovie.ai_platform.agentic_ai.dto.request.UpdateSessionNameDto;
import bbmovie.ai_platform.agentic_ai.dto.response.ChatSessionResponse;
import bbmovie.ai_platform.agentic_ai.service.session.SessionService;
import lombok.RequiredArgsConstructor;

import com.bbmovie.common.dtos.ApiResponse;
import com.bbmovie.common.dtos.CursorPageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<CursorPageResponse<ChatSessionResponse>>> getSessions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return sessionService.activeSessionsWithCursor(userId, cursor, size)
                .map(ApiResponse::success);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<ChatSessionResponse>> createSession(
            @RequestBody CreateSessionDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return sessionService.createSession(userId, dto.sessionName())
                .map(ApiResponse::success);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ApiResponse<String>> deleteSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return sessionService.deleteSession(sessionId, userId)
                .then(Mono.fromCallable(() -> ApiResponse.success("Session deleted successfully")));
    }

    @PatchMapping("/{sessionId}/name")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<ChatSessionResponse>> updateSessionName(
            @PathVariable UUID sessionId,
            @RequestBody UpdateSessionNameDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return sessionService.updateSessionName(sessionId, userId, dto.newName())
                .map(ApiResponse::success);
    }

    @PatchMapping("/{sessionId}/archived")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<ChatSessionResponse>> setSessionArchived(
            @PathVariable UUID sessionId,
            @RequestBody SetSessionArchivedDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return sessionService.setSessionArchived(sessionId, userId, dto.archived())
                .map(ApiResponse::success);
    }

    @GetMapping("/archived")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<CursorPageResponse<ChatSessionResponse>>> listArchivedSessions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return sessionService.listArchivedSessions(userId, cursor, size)
                .map(ApiResponse::success);
    }

    @DeleteMapping("/archived")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<String>> deleteArchivedSessions(
            @RequestBody DeleteArchivedSessionsDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return sessionService.deleteArchivedSessions(dto.sessionIds(), userId)
                .then(Mono.fromCallable(() -> ApiResponse.success("Archived sessions deleted successfully")));
    }
}
