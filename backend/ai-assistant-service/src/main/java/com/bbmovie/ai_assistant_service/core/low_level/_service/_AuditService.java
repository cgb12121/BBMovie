package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._entity._AiInteractionAudit;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._repository._AiInteractionAuditRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class _AuditService {

    private final _AiInteractionAuditRepository repository;
    private final ObjectMapper objectMapper;

    public Mono<Void> recordInteraction(UUID sessionId, _InteractionType type, Object details) {
        return Mono.fromCallable(() -> {
            String detailsJson = objectMapper.writeValueAsString(details);

            return _AiInteractionAudit.builder()
                    .sessionId(sessionId)
                    .interactionType(type)
                    .timestamp(Instant.now())
                    .details(detailsJson)
                    .build();
        })
        .flatMap(repository::save)
        .doOnSuccess(saved -> log.debug("Audit record saved: {}", saved.getId()))
        .doOnError(e -> log.error("Failed to save audit record for session {}", sessionId, e))
        .then();
    }
}
