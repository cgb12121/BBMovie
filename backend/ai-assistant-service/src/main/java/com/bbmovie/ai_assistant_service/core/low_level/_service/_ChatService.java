package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._SessionNotFoundException;
import com.bbmovie.ai_assistant_service.core.low_level._assistant._Assistant;
import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatSession;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model.AssistantType;
import com.bbmovie.ai_assistant_service.core.low_level._repository._ChatMessageRepository;
import com.bbmovie.ai_assistant_service.core.low_level._repository._ChatSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.common.entity.JoseConstraint.JosePayload.ROLE;
import static com.example.common.entity.JoseConstraint.JosePayload.SUB;

@Slf4j
@Service
public class _ChatService {

    private final Map<AssistantType, _Assistant> assistants;
    private final _ChatSessionRepository sessionRepository;
    private final _ChatMessageRepository messageRepository;

    public _ChatService(
            List<_Assistant> assistantList,
            _ChatSessionRepository sessionRepository,
            _ChatMessageRepository messageRepository) {
        this.assistants = assistantList.stream()
                .collect(Collectors.toUnmodifiableMap(_Assistant::getType, Function.identity()));
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        log.info("Initialized ChatService with assistants: {}", this.assistants.keySet());
    }

    public Flux<String> chat(UUID sessionId, String message, AssistantType assistantType, Jwt jwt) {
        UUID userId = jwt.getClaim(SUB);
        // TODO: Extract user role from Principal
        String userRole = jwt.getClaim(ROLE);

        return validateSessionOwnership(sessionId, userId)
                .flatMapMany(session -> {
                    _Assistant assistant = assistants.get(assistantType);
                    if (assistant == null) {
                        return Flux.error(new IllegalArgumentException("Unknown assistant type: " + assistantType));
                    }
                    return assistant.processMessage(sessionId, message, userRole);
                });
    }

    public Flux<_ChatSession> listSessionsForUser(UUID userId) {
        return sessionRepository.findByUserId(userId);
    }

    public Mono<_ChatSession> createSession(UUID userId, String sessionName) {
        _ChatSession session = _ChatSession.builder()
                .userId(userId)
                .sessionName(sessionName)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return sessionRepository.save(session);
    }

    public Mono<Void> deleteSession(UUID sessionId, UUID userId) {
        return validateSessionOwnership(sessionId, userId)
                .flatMap(session -> messageRepository.deleteBySessionId(session.getId())
                        .then(sessionRepository.deleteById(session.getId()))
                );
    }

    private Mono<_ChatSession> validateSessionOwnership(UUID sessionId, UUID userId) {
        return sessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new _SessionNotFoundException("Session not found")))
                .flatMap(session -> {
                    if (!session.getUserId().equals(userId)) {
                        log.warn("Access denied: User {} attempted to access session {} owned by {}",
                                userId, sessionId, session.getUserId());
                        return Mono.error(new SecurityException("User does not own session " + sessionId));
                    }
                    return Mono.just(session);
                });
    }
}
