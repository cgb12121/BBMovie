package bbmovie.ai_platform.agentic_ai.aop;

import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import bbmovie.ai_platform.agentic_ai.repository.SessionRepository;
import bbmovie.ai_platform.aop_policy.service.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * OwnershipValidatorImpl is a security component that validates if a user owns a specific resource.
 * 
 * It is primarily used by AOP-based authorization aspects to ensure that users can only 
 * access or modify their own sessions and messages.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OwnershipValidatorImpl implements OwnershipValidator {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;

    /**
     * Checks if the given user ID is the owner of the resource identified by resourceId.
     * 
     * @param userId The ID of the user requesting access.
     * @param resourceId The ID of the resource (Session or Message).
     * @param entityType The type of resource ("SESSION" or "MESSAGE").
     * @return Mono<Boolean> true if the user is the owner, false otherwise.
     */
    @Override
    public Mono<Boolean> checkOwnership(UUID userId, UUID resourceId, String entityType) {
        log.debug("[Ownership] Checking {} ownership for User: {}, Resource: {}", entityType, userId, resourceId);
        
        return switch (entityType.toUpperCase()) {
            case "SESSION" -> sessionRepository.findById(resourceId)
                    .map(session -> session.getUserId().equals(userId))
                    .defaultIfEmpty(false);
            
            case "MESSAGE" -> messageRepository.findById(resourceId)
                    .map(message -> message.getUserId().equals(userId))
                    .defaultIfEmpty(false);
            
            default -> {
                log.warn("[Ownership] Unknown entity type: {}", entityType);
                yield Mono.just(false);
            }
        };
    }
}
