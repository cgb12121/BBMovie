package bbmovie.ai_platform.agentic_ai.service;

import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import bbmovie.ai_platform.agentic_ai.repository.SessionRepository;
import bbmovie.ai_platform.aop_policy.service.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnershipValidatorImpl implements OwnershipValidator {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;

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
