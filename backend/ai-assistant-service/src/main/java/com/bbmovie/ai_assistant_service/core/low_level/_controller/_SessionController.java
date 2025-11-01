package com.bbmovie.ai_assistant_service.core.low_level._controller;

import com.bbmovie.ai_assistant_service.core.low_level._dto._CreateSessionDto;
import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatSession;
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
    public Mono<ApiResponse<CursorPageResponse<_ChatSession>>> getSessions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        return sessionService.listSessionsWithCursor(userId, cursor, size)
                .map(ApiResponse::success);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<_ChatSession>> createSession(
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

    //TODO: missing session update: archive, rename, etc
}
