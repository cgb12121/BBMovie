package bbmovie.ai_platform.agentic_ai.service.personalize;

import bbmovie.ai_platform.agentic_ai.dto.request.UpdateAgentConfigDto;
import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Service for managing user personalization, identity briefs, and behavior facts.
 */
public interface PersonalizationService {

    /**
     * Generates a personalized system prompt fragment for the given user and session.
     * Implements L1-L2-L3 caching logic.
     * 
     * @param userId The ID of the user.
     * @param sessionId The current chat session ID.
     * @return Mono emitting a string containing the personalized context.
     */
    Mono<String> getPersonalizedContext(UUID userId, UUID sessionId);

    /**
     * Records a new behavior or fact about the user.
     * Updates L3 (Vector DB) and invalidates L1/L2 caches.
     * 
     * @param userId The ID of the user.
     * @param fact The new fact to record.
     * @return Mono signaling completion.
     */
    Mono<Void> recordBehavior(UUID userId, String fact);

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
     * @param dto The configuration update DTO.
     * @return Mono signaling completion.
     */
    Mono<Void> updateCustomInstructions(UUID userId, UpdateAgentConfigDto dto);
}
