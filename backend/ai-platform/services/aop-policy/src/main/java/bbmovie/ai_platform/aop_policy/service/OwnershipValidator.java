package bbmovie.ai_platform.aop_policy.service;

import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Interface for verifying resource ownership.
 * Implemented by services that manage ownable resources.
 */
public interface OwnershipValidator {
    
    /**
     * Verifies if the given user owns the specified resource.
     * 
     * @param userId The ID of the user attempting access.
     * @param resourceId The ID of the resource (Session, Message, etc.).
     * @param entityType The type of entity.
     * @return Mono that emits true if ownership is valid, false otherwise.
     */
    Mono<Boolean> checkOwnership(UUID userId, UUID resourceId, String entityType);
}
