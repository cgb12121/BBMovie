package bbmovie.ai_platform.agentic_ai.controller;

import bbmovie.ai_platform.agentic_ai.dto.MemoryRecord;
import bbmovie.ai_platform.agentic_ai.service.personalize.PersonalizationService;
import com.bbmovie.common.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST API for users to view and manage their AI-persisted memory records.
 *
 * <p>Users can:
 * <ul>
 *   <li>List their memories (GET) — to see what the AI has learned about them</li>
 *   <li>Delete a specific memory (DELETE) — to correct or prune unwanted facts</li>
 * </ul>
 *
 * <p>Editing is intentionally not supported — users should delete and let the AI re-learn.
 * The per-user cap ({@code MAX_MEMORIES_PER_USER}) is surfaced via the list response size.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/memories")
public class MemoryController {

    private final PersonalizationService personalizationService;

    /**
     * Returns all AI-saved memory records for the authenticated user, most recent first.
     * The client can use the response size to determine if the cap has been reached.
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<ApiResponse<MemoryRecord>> listMemories(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return personalizationService.getMemories(userId)
                .map(ApiResponse::success);
    }

    /**
     * Deletes a specific memory record owned by the authenticated user.
     * Returns 200 regardless of whether the record existed (idempotent).
     * The Qdrant semantic index is intentionally NOT modified — it serves as the
     * AI's long-term semantic memory, separate from this user-managed list.
     */
    @DeleteMapping("/{memoryId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<String>> deleteMemory(
            @PathVariable UUID memoryId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return personalizationService.deleteMemory(memoryId, userId)
                .then(Mono.fromCallable(() -> ApiResponse.success("Memory deleted successfully")));
    }
}
