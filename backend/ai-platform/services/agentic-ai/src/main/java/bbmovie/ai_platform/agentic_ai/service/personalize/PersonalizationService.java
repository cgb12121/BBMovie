package bbmovie.ai_platform.agentic_ai.service.personalize;

import bbmovie.ai_platform.agentic_ai.dto.MemoryRecord;
import bbmovie.ai_platform.agentic_ai.dto.request.UpdateAgentConfigDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service for managing user personalization, identity briefs, and behavior facts.
 */
public interface PersonalizationService {

    /**
     * Generates a personalized system prompt fragment for the given user.
     * Implements L1 (Caffeine, keyed by userId) → L2 (Redis) → L3 (SQL + Vector DB) caching logic.
     *
     * @param userId The ID of the user.
     * @return Mono emitting a string containing the personalized context.
     */
    Mono<String> getPersonalizedContext(UUID userId);

    /**
     * Records a new behavior or fact about the user into the Vector DB (Qdrant).
     * This is the AI's long-term semantic memory, separate from the user-visible memory cap.
     * Invalidates L1/L2 caches so the next request picks up the new context.
     *
     * @param userId The ID of the user.
     * @param fact   The new fact to record (should be concise).
     * @return Mono signaling completion.
     */
    Mono<Void> recordBehavior(UUID userId, String fact);

    /**
     * Retrieves the user-visible list of AI-saved memory records from PostgreSQL.
     * Ordered by most recent first.
     *
     * @param userId The ID of the user.
     * @return Flux of {@link MemoryRecord} DTOs.
     */
    Flux<MemoryRecord> getMemories(UUID userId);

    /**
     * Deletes a specific memory record from PostgreSQL and invalidates caches.
     * Only the owning user can delete their own memory.
     * Note: the corresponding Qdrant vector is intentionally NOT removed — Qdrant serves
     * as a long-term semantic index separate from the user-managed explicit memory list.
     *
     * @param memoryId The ID of the memory record to delete.
     * @param userId   The requesting user's ID (used for ownership enforcement).
     * @return Mono signaling completion, or empty if not found / not owned.
     */
    Mono<Void> deleteMemory(UUID memoryId, UUID userId);

    /**
     * Retrieves the custom instructions (system prompt overrides) for a user.
     *
     * @param userId The ID of the user.
     * @return Mono emitting the custom instructions string.
     */
    Mono<String> getCustomInstructions(UUID userId);

    /**
     * Updates the custom instructions and other agent configurations for a user.
     *
     * @param userId The ID of the user.
     * @param dto    The configuration update DTO.
     * @return Mono signaling completion.
     */
    Mono<Void> updateCustomInstructions(UUID userId, UpdateAgentConfigDto dto);
}
