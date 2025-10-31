package com.bbmovie.ai_assistant_service.core.low_level._controller;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatSession;
import com.bbmovie.ai_assistant_service.core.low_level._service._ChatSessionService;
import com.bbmovie.ai_assistant_service.shared_dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/session")
public class _SessionController {

    private final _ChatSessionService sessionService;

    @Autowired
    public _SessionController(_ChatSessionService sessionService) {
        this.sessionService = sessionService;
    }

    //TODO: this endpoint might be removed and the agent will auto create new session
    @PostMapping
    public ResponseEntity<ApiResponse<Mono<_ChatSession>>> newSession(UUID userId, String sessionName) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.newSession(userId, sessionName)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Flux<_ChatSession>>> getAllSession(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.findAll(page, size)));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Mono<Void>>> deleteSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.deleteSession(sessionId)));
    }

    @PutMapping("/{sessionId}/archive")
    public ResponseEntity<ApiResponse<Mono<Void>>> archiveSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.archiveSession(sessionId)));
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Mono<_ChatSession>>> renameSession(@PathVariable UUID sessionId, @RequestBody String newName) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.renameSession(sessionId, newName)));
    }
}
